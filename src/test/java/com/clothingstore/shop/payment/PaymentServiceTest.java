package com.clothingstore.shop.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    private final PaymentRepository repository = mock(PaymentRepository.class);
    private final PaymentWebhookVerifier verifier = mock(PaymentWebhookVerifier.class);
    private final PaymentService service = new PaymentService(repository, verifier, new ObjectMapper());

    private final UUID intentId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private PaymentRepository.Intent processing;
    private PaymentRepository.Attempt pending;

    @BeforeEach
    void setUp() {
        processing = new PaymentRepository.Intent(intentId, 42, 7, 1000, "TWD", PaymentState.PROCESSING, "create-key");
        pending = new PaymentRepository.Attempt(attemptId, intentId, "capture-key", PaymentAttemptState.PENDING, null, null);
    }

    @Test
    void replaysCreateKeyButRejectsReuseForAnotherOrder() {
        PaymentRepository.OrderSnapshot order = new PaymentRepository.OrderSnapshot(42, 1000, "Card", "PENDING");
        when(repository.findOwnedOrder(42, 7)).thenReturn(order);
        PaymentRepository.Intent existing = new PaymentRepository.Intent(intentId, 42, 7, 1000, "TWD",
                PaymentState.REQUIRES_PAYMENT, "create-key");
        when(repository.findByCustomerKey(7, "create-key")).thenReturn(existing);

        assertEquals(intentId, service.createIntent(7, 42, "create-key").intentId());
        verify(repository, never()).insertIntent(any(), anyInt(), anyInt(), anyInt(), anyString());

        when(repository.findOwnedOrder(43, 7)).thenReturn(new PaymentRepository.OrderSnapshot(43, 1000, "Card", "PENDING"));
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.createIntent(7, 43, "create-key")).getStatusCode().value());
    }

    @Test
    void captureIsIdempotentAndBlocksConcurrentDifferentKey() {
        when(repository.findOwnedIntent(intentId, 7)).thenReturn(processing);
        when(repository.lockIntent(intentId)).thenReturn(processing);
        when(repository.findAttemptByKey(intentId, "capture-key")).thenReturn(pending);

        assertEquals(attemptId, service.requestCapture(7, intentId, "capture-key").attemptId());
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.requestCapture(7, intentId, "different-key")).getStatusCode().value());
        verify(repository, never()).insertAttempt(any(), any(), anyString());
    }

    @Test
    void verifiedCaptureMarksOrderPaidOnce() {
        String body = event("event-1", "CAPTURE_SUCCEEDED", "provider-123");
        when(repository.findAttempt(attemptId)).thenReturn(pending);
        when(repository.lockIntent(intentId)).thenReturn(processing);
        when(repository.insertEvent(eq("event-1"), eq(attemptId), eq("CAPTURE_SUCCEEDED"), anyString())).thenReturn(true);
        when(repository.markOrderPaid(42, 1000)).thenReturn(1);

        PaymentView result = service.receiveProviderEvent("timestamp", "signature", body);

        assertEquals(PaymentState.CAPTURED, result.status());
        assertEquals(PaymentAttemptState.SUCCEEDED, result.attemptStatus());
        verify(verifier).verify("timestamp", "signature", body);
        verify(repository).resolveAttempt(attemptId, PaymentAttemptState.SUCCEEDED, "provider-123", null);
        verify(repository).updateIntentState(intentId, PaymentState.CAPTURED);
        verify(repository, times(1)).markOrderPaid(42, 1000);
    }

    @Test
    void duplicateProviderEventDoesNotRepeatPaymentWrites() throws Exception {
        String body = event("event-1", "CAPTURE_SUCCEEDED", "provider-123");
        when(repository.findAttempt(attemptId)).thenReturn(pending);
        when(repository.lockIntent(intentId)).thenReturn(processing);
        when(repository.findEventDigest("event-1")).thenReturn(sha256(body));

        assertEquals(PaymentState.PROCESSING, service.receiveProviderEvent("timestamp", "signature", body).status());
        verify(repository, never()).insertEvent(anyString(), any(), anyString(), anyString());
        verify(repository, never()).markOrderPaid(anyInt(), anyInt());
        verify(repository, never()).updateIntentState(any(), any());
    }

    @Test
    void wrongAmountNeverUpdatesPayment() {
        String body = "{\"eventId\":\"event-1\",\"attemptId\":\"" + attemptId + "\","
                + "\"type\":\"CAPTURE_SUCCEEDED\",\"amount\":999,\"currency\":\"TWD\","
                + "\"providerReference\":\"provider-123\"}";
        when(repository.findAttempt(attemptId)).thenReturn(pending);
        when(repository.lockIntent(intentId)).thenReturn(processing);

        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.receiveProviderEvent("timestamp", "signature", body)).getStatusCode().value());
        verify(repository, never()).insertEvent(anyString(), any(), anyString(), anyString());
        verify(repository, never()).markOrderPaid(anyInt(), anyInt());
    }

    @Test
    void settlementOnlyFollowsSuccessfulCapture() {
        PaymentRepository.Intent captured = new PaymentRepository.Intent(intentId, 42, 7, 1000, "TWD",
                PaymentState.CAPTURED, "create-key");
        PaymentRepository.Attempt succeeded = new PaymentRepository.Attempt(attemptId, intentId, "capture-key",
                PaymentAttemptState.SUCCEEDED, "provider-123", null);
        String body = event("event-2", "SETTLED", "provider-123");
        when(repository.findAttempt(attemptId)).thenReturn(succeeded);
        when(repository.lockIntent(intentId)).thenReturn(captured);
        when(repository.insertEvent(eq("event-2"), eq(attemptId), eq("SETTLED"), anyString())).thenReturn(true);

        assertEquals(PaymentState.SETTLED, service.receiveProviderEvent("timestamp", "signature", body).status());
        verify(repository).updateIntentState(intentId, PaymentState.SETTLED);
        verify(repository, never()).markOrderPaid(anyInt(), anyInt());
    }

    @Test
    void failedCaptureCanBeRetriedWithNewKey() {
        PaymentRepository.Intent failed = new PaymentRepository.Intent(intentId, 42, 7, 1000, "TWD",
                PaymentState.FAILED, "create-key");
        PaymentRepository.Attempt retry = new PaymentRepository.Attempt(UUID.randomUUID(), intentId, "new-capture-key",
                PaymentAttemptState.PENDING, null, null);
        when(repository.findOwnedIntent(intentId, 7)).thenReturn(failed);
        when(repository.lockIntent(intentId)).thenReturn(failed);
        when(repository.findOwnedOrder(42, 7))
                .thenReturn(new PaymentRepository.OrderSnapshot(42, 1000, "Card", "PENDING"));
        when(repository.insertAttempt(any(), eq(intentId), eq("new-capture-key"))).thenReturn(retry);

        PaymentView result = service.requestCapture(7, intentId, "new-capture-key");

        assertEquals(PaymentState.PROCESSING, result.status());
        assertEquals(retry.id(), result.attemptId());
        verify(repository).updateIntentState(intentId, PaymentState.PROCESSING);
    }

    @Test
    void crossCustomerIntentAccessIsHidden() {
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.requestCapture(99, intentId, "new-capture-key")).getStatusCode().value());
        verify(repository, never()).lockIntent(any());
    }

    @Test
    void changedReplayPayloadIsRejected() {
        String body = event("event-1", "CAPTURE_SUCCEEDED", "provider-123");
        when(repository.findAttempt(attemptId)).thenReturn(pending);
        when(repository.lockIntent(intentId)).thenReturn(processing);
        when(repository.findEventDigest("event-1")).thenReturn("some-other-digest");

        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.receiveProviderEvent("timestamp", "signature", body)).getStatusCode().value());
        verify(repository, never()).markOrderPaid(anyInt(), anyInt());
    }

    private String event(String id, String type, String reference) {
        return "{\"eventId\":\"" + id + "\",\"attemptId\":\"" + attemptId + "\","
                + "\"type\":\"" + type + "\",\"amount\":1000,\"currency\":\"TWD\","
                + "\"providerReference\":\"" + reference + "\"}";
    }

    private String sha256(String body) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(body.getBytes(StandardCharsets.UTF_8)));
    }
}
