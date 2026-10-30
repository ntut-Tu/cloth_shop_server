package com.clothingstore.shop.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository repository;
    private final PaymentWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository repository, PaymentWebhookVerifier webhookVerifier, ObjectMapper objectMapper) {
        this.repository = repository;
        this.webhookVerifier = webhookVerifier;
        this.objectMapper = objectMapper;
    }

    public record ProviderEvent(String eventId, UUID attemptId, String type,
                                Integer amount, String currency, String providerReference, String failureCode) {}

    @Transactional
    public PaymentView createIntent(int userId, int orderId, String idempotencyKey) {
        requireKey(idempotencyKey);
        PaymentRepository.OrderSnapshot order = repository.findOwnedOrder(orderId, userId);
        if (order == null) throw notFound();
        if (!"Card".equals(order.method())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment intents require a card order");
        }
        PaymentRepository.Intent byKey = repository.findByCustomerKey(userId, idempotencyKey);
        if (byKey != null) {
            if (byKey.orderId() != orderId) throw conflict("Idempotency key is already used for another order");
            return view(byKey, repository.findLatestAttempt(byKey.id()));
        }
        PaymentRepository.Intent byOrder = repository.findByOrder(orderId);
        if (byOrder != null) throw conflict("Order already has a payment intent");
        if (!"PENDING".equals(order.payStatus()) || order.amount() <= 0) {
            throw conflict("Order is not eligible for payment");
        }
        PaymentRepository.Intent created = repository.insertIntent(UUID.randomUUID(), orderId, userId, order.amount(), idempotencyKey);
        if (created == null) {
            PaymentRepository.Intent existing = repository.findByCustomerKey(userId, idempotencyKey);
            if (existing != null && existing.orderId() == orderId) {
                return view(existing, repository.findLatestAttempt(existing.id()));
            }
            throw conflict("Order already has a payment intent");
        }
        return view(created, null);
    }

    @Transactional(readOnly = true)
    public PaymentView getIntent(int userId, UUID intentId) {
        PaymentRepository.Intent intent = repository.findOwnedIntent(intentId, userId);
        if (intent == null) throw notFound();
        return view(intent, repository.findLatestAttempt(intent.id()));
    }

    @Transactional
    public PaymentView requestCapture(int userId, UUID intentId, String idempotencyKey) {
        requireKey(idempotencyKey);
        PaymentRepository.Intent intent = lockOwnedIntent(userId, intentId);
        PaymentRepository.Attempt existing = repository.findAttemptByKey(intentId, idempotencyKey);
        if (existing != null) return view(intent, existing);
        if (intent.status() != PaymentState.REQUIRES_PAYMENT && intent.status() != PaymentState.FAILED) {
            throw conflict("Payment intent cannot start another capture");
        }
        PaymentRepository.OrderSnapshot order = repository.findOwnedOrder(intent.orderId(), userId);
        if (order == null || !"PENDING".equals(order.payStatus()) || order.amount() != intent.amount()) {
            throw conflict("Order is not eligible for capture");
        }
        PaymentRepository.Attempt attempt = repository.insertAttempt(UUID.randomUUID(), intentId, idempotencyKey);
        if (attempt == null) throw conflict("Capture key is already in use");
        repository.updateIntentState(intentId, PaymentState.PROCESSING);
        return view(withStatus(intent, PaymentState.PROCESSING), attempt);
    }

    @Transactional
    public PaymentView cancelIntent(int userId, UUID intentId) {
        PaymentRepository.Intent intent = lockOwnedIntent(userId, intentId);
        if (intent.status() == PaymentState.CANCELLED) {
            return view(intent, repository.findLatestAttempt(intentId));
        }
        if (intent.status() != PaymentState.REQUIRES_PAYMENT && intent.status() != PaymentState.FAILED) {
            throw conflict("Payment intent cannot be cancelled while capture is in progress or completed");
        }
        repository.updateIntentState(intentId, PaymentState.CANCELLED);
        return view(withStatus(intent, PaymentState.CANCELLED), repository.findLatestAttempt(intentId));
    }

    @Transactional
    public PaymentView receiveProviderEvent(String timestamp, String signature, String rawBody) {
        webhookVerifier.verify(timestamp, signature, rawBody);
        ProviderEvent event = parseEvent(rawBody);
        validateEvent(event);
        PaymentRepository.Attempt initialAttempt = repository.findAttempt(event.attemptId());
        if (initialAttempt == null) throw notFound();
        PaymentRepository.Intent intent = repository.lockIntent(initialAttempt.intentId());
        PaymentRepository.Attempt attempt = repository.findAttempt(event.attemptId());
        if (intent == null || attempt == null || !attempt.intentId().equals(intent.id())) throw notFound();
        if (!event.amount().equals(intent.amount()) || !event.currency().equals(intent.currency())) {
            throw conflict("Provider event amount or currency does not match the payment intent");
        }
        String digest = sha256(rawBody);
        String priorDigest = repository.findEventDigest(event.eventId());
        if (priorDigest != null) {
            if (!priorDigest.equals(digest)) throw conflict("Provider event ID was reused with a different payload");
            return view(intent, attempt);
        }
        if (!repository.insertEvent(event.eventId(), attempt.id(), event.type(), digest)) {
            throw conflict("Provider event ID is already in use");
        }
        return switch (event.type()) {
            case "CAPTURE_SUCCEEDED" -> captureSucceeded(intent, attempt, event);
            case "CAPTURE_FAILED" -> captureFailed(intent, attempt, event);
            case "SETTLED" -> settled(intent, attempt, event);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown payment event type");
        };
    }

    private PaymentView captureSucceeded(PaymentRepository.Intent intent, PaymentRepository.Attempt attempt, ProviderEvent event) {
        requireReference(event.providerReference());
        if (attempt.status() == PaymentAttemptState.SUCCEEDED
                && event.providerReference().equals(attempt.providerReference())
                && (intent.status() == PaymentState.CAPTURED || intent.status() == PaymentState.SETTLED)) {
            return view(intent, attempt);
        }
        if (attempt.status() != PaymentAttemptState.PENDING || intent.status() != PaymentState.PROCESSING) {
            throw conflict("Capture success conflicts with payment state");
        }
        repository.resolveAttempt(attempt.id(), PaymentAttemptState.SUCCEEDED, event.providerReference(), null);
        repository.updateIntentState(intent.id(), PaymentState.CAPTURED);
        if (repository.markOrderPaid(intent.orderId(), intent.amount()) != 1) {
            throw conflict("Order payment status could not be updated");
        }
        return view(withStatus(intent, PaymentState.CAPTURED),
                new PaymentRepository.Attempt(attempt.id(), attempt.intentId(), attempt.idempotencyKey(),
                        PaymentAttemptState.SUCCEEDED, event.providerReference(), null));
    }

    private PaymentView captureFailed(PaymentRepository.Intent intent, PaymentRepository.Attempt attempt, ProviderEvent event) {
        if (attempt.status() == PaymentAttemptState.FAILED && intent.status() == PaymentState.FAILED) {
            return view(intent, attempt);
        }
        if (attempt.status() != PaymentAttemptState.PENDING || intent.status() != PaymentState.PROCESSING) {
            throw conflict("Capture failure conflicts with payment state");
        }
        repository.resolveAttempt(attempt.id(), PaymentAttemptState.FAILED, null, event.failureCode());
        repository.updateIntentState(intent.id(), PaymentState.FAILED);
        return view(withStatus(intent, PaymentState.FAILED),
                new PaymentRepository.Attempt(attempt.id(), attempt.intentId(), attempt.idempotencyKey(),
                        PaymentAttemptState.FAILED, null, event.failureCode()));
    }

    private PaymentView settled(PaymentRepository.Intent intent, PaymentRepository.Attempt attempt, ProviderEvent event) {
        requireReference(event.providerReference());
        if (attempt.status() != PaymentAttemptState.SUCCEEDED
                || !event.providerReference().equals(attempt.providerReference())) {
            throw conflict("Settlement does not match a captured attempt");
        }
        if (intent.status() == PaymentState.SETTLED) return view(intent, attempt);
        if (intent.status() != PaymentState.CAPTURED) throw conflict("Payment has not been captured");
        repository.updateIntentState(intent.id(), PaymentState.SETTLED);
        return view(withStatus(intent, PaymentState.SETTLED), attempt);
    }

    private PaymentRepository.Intent lockOwnedIntent(int userId, UUID intentId) {
        if (repository.findOwnedIntent(intentId, userId) == null) throw notFound();
        PaymentRepository.Intent locked = repository.lockIntent(intentId);
        if (locked == null || locked.customerUserId() != userId) throw notFound();
        return locked;
    }

    private PaymentRepository.Intent withStatus(PaymentRepository.Intent intent, PaymentState status) {
        return new PaymentRepository.Intent(intent.id(), intent.orderId(), intent.customerUserId(), intent.amount(),
                intent.currency(), status, intent.createKey());
    }

    private PaymentView view(PaymentRepository.Intent intent, PaymentRepository.Attempt attempt) {
        return new PaymentView(intent.id(), intent.orderId(), intent.amount(), intent.currency(), intent.status(),
                attempt == null ? null : attempt.id(), attempt == null ? null : attempt.status());
    }

    private ProviderEvent parseEvent(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, ProviderEvent.class);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment provider event");
        }
    }

    private void validateEvent(ProviderEvent event) {
        if (event.eventId() == null || event.eventId().isBlank() || event.eventId().length() > 128
                || event.attemptId() == null || event.amount() == null || event.amount() <= 0
                || event.currency() == null || event.currency().length() != 3
                || event.type() == null || event.failureCode() != null && event.failureCode().length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment provider event");
        }
        if (!event.type().equals("CAPTURE_SUCCEEDED") && !event.type().equals("CAPTURE_FAILED")
                && !event.type().equals("SETTLED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown payment event type");
        }
    }

    private void requireReference(String reference) {
        if (reference == null || reference.isBlank() || reference.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provider reference is required");
        }
    }

    private void requireKey(String key) {
        if (key == null || key.length() < 8 || key.length() > 128 || !key.matches("[A-Za-z0-9._:-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key must be 8-128 safe characters");
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment resource not found");
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
