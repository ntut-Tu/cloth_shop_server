package com.clothingstore.shop.returns;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ReturnCaseService {
    private final ReturnCaseRepository repository;

    public ReturnCaseService(ReturnCaseRepository repository) {
        this.repository = repository;
    }

    public record Actor(int userId, String role) {}

    @Transactional
    public ReturnCaseModels.Detail create(Actor actor, ReturnCaseModels.CreateRequest request, String key) {
        requireRole(actor, "customer");
        requireKey(key);
        if (request == null || request.orderItemId() == null || request.orderItemId() <= 0
                || request.quantity() == null || request.quantity() <= 0) {
            throw badRequest("A valid order item and positive quantity are required");
        }
        String reason = requireNote(request.reason());
        String hash = digest(request.orderItemId() + "|" + request.quantity() + "|" + reason);
        ReturnCaseRepository.Case existing = repository.findByCreateKey(actor.userId(), key);
        if (existing != null) {
            if (!existing.requestHash().equals(hash)) throw conflict("Idempotency key has a different request");
            return detail(existing);
        }

        ReturnCaseRepository.Item item = repository.lockOwnedItem(request.orderItemId(), actor.userId());
        if (item == null) throw notFound();
        ReturnCaseRepository.Case concurrentReplay = repository.findByCreateKey(actor.userId(), key);
        if (concurrentReplay != null) {
            if (!concurrentReplay.requestHash().equals(hash)) throw conflict("Idempotency key has a different request");
            return detail(concurrentReplay);
        }
        if (!"PAID".equalsIgnoreCase(item.payStatus())
                || !("Delivered".equalsIgnoreCase(item.shipStatus()) || "Completed".equalsIgnoreCase(item.shipStatus()))) {
            throw conflict("Order item is not eligible for return");
        }
        if (repository.hasLegacyRefund(item.itemId())) {
            throw conflict("Order item already has a legacy refund request");
        }
        int reserved = repository.reservedQuantity(item.itemId());
        if (reserved < 0 || request.quantity() > item.quantity() - reserved) {
            throw conflict("Requested quantity exceeds the unclaimed purchased quantity");
        }
        if (item.unitPrice() < 0 || item.storeSubtotal() <= 0 || item.storeNet() < 0
                || item.storeNet() > item.storeSubtotal()) {
            throw conflict("Order pricing is not eligible for return settlement");
        }
        long gross = (long) item.unitPrice() * request.quantity();
        if (gross > Integer.MAX_VALUE) {
            throw conflict("Return amount exceeds supported range");
        }
        UUID caseId = UUID.randomUUID();
        OffsetDateTime vendorDeadline = OffsetDateTime.now(ZoneOffset.UTC).plusDays(7);
        ReturnCaseRepository.Case created = repository.createCase(caseId, item, actor.userId(), request.quantity(),
                (int) gross, reason, key, hash, vendorDeadline);
        if (created == null) {
            ReturnCaseRepository.Case raced = repository.findByCreateKey(actor.userId(), key);
            if (raced != null && raced.requestHash().equals(hash)) return detail(raced);
            throw conflict("Return request already exists");
        }
        if (!repository.appendAction(caseId, actor.userId(), actor.role(), key, "CREATED", null,
                ReturnCaseState.VENDOR_PENDING, hash, reason)) {
            throw conflict("Action key already used");
        }
        return detail(created);
    }

    @Transactional(readOnly = true)
    public ReturnCaseModels.Detail get(Actor actor, UUID caseId) {
        ReturnCaseRepository.Case entry = repository.findCase(caseId);
        requireAccess(actor, entry);
        return detail(entry);
    }

    @Transactional(readOnly = true)
    public List<ReturnCaseModels.Summary> list(Actor actor, int page, int size) {
        if (page < 1 || size < 1 || size > 100 || (long) (page - 1) * size > Integer.MAX_VALUE) {
            throw badRequest("Invalid pagination");
        }
        requireKnownRole(actor);
        return repository.list(actor.role(), actor.userId(), size, (page - 1) * size)
                .stream().map(this::summary).toList();
    }

    @Transactional
    public ReturnCaseModels.Detail vendorDecision(Actor actor, UUID caseId,
                                                   ReturnCaseModels.DecisionRequest request, String key) {
        requireRole(actor, "vendor");
        if (request == null || request.approved() == null) throw badRequest("Decision is required");
        String note = requireNote(request.response());
        ReturnCaseRepository.Case entry = lockAccessible(actor, caseId);
        String type = request.approved() ? "VENDOR_APPROVE" : "VENDOR_REJECT";
        ReturnCaseModels.Detail replay = replayIfPresent(actor, entry, key, type, note);
        if (replay != null) return replay;
        if (entry.status() != ReturnCaseState.VENDOR_PENDING) throw conflict("Vendor decision is no longer pending");
        if (!OffsetDateTime.now(ZoneOffset.UTC).isBefore(entry.vendorDeadline())) {
            throw conflict("Vendor decision deadline has passed");
        }
        ReturnCaseState next = request.approved() ? ReturnCaseState.VENDOR_APPROVED : ReturnCaseState.VENDOR_REJECTED;
        OffsetDateTime appealDeadline = request.approved() ? null : OffsetDateTime.now(ZoneOffset.UTC).plusDays(7);
        return apply(actor, entry, key, type, note, next, note, null, appealDeadline, request.approved());
    }

    @Transactional
    public ReturnCaseModels.Detail appeal(Actor actor, UUID caseId, ReturnCaseModels.AppealRequest request, String key) {
        requireRole(actor, "customer");
        String note = requireNote(request == null ? null : request.reason());
        ReturnCaseRepository.Case entry = lockAccessible(actor, caseId);
        ReturnCaseModels.Detail replay = replayIfPresent(actor, entry, key, "APPEAL", note);
        if (replay != null) return replay;
        if (entry.status() != ReturnCaseState.VENDOR_REJECTED) throw conflict("Case cannot be appealed");
        if (entry.appealDeadline() == null || !OffsetDateTime.now(ZoneOffset.UTC).isBefore(entry.appealDeadline())) {
            throw conflict("Appeal deadline has passed");
        }
        return apply(actor, entry, key, "APPEAL", note, ReturnCaseState.ADMIN_PENDING,
                null, null, null, false);
    }

    @Transactional
    public ReturnCaseModels.Detail adminDecision(Actor actor, UUID caseId,
                                                  ReturnCaseModels.DecisionRequest request, String key) {
        requireRole(actor, "admin");
        if (request == null || request.approved() == null) throw badRequest("Decision is required");
        String note = requireNote(request.response());
        ReturnCaseRepository.Case entry = lockAccessible(actor, caseId);
        String type = request.approved() ? "ADMIN_APPROVE" : "ADMIN_REJECT";
        ReturnCaseModels.Detail replay = replayIfPresent(actor, entry, key, type, note);
        if (replay != null) return replay;
        if (entry.status() != ReturnCaseState.ADMIN_PENDING) throw conflict("Admin decision is no longer pending");
        ReturnCaseState next = request.approved() ? ReturnCaseState.ADMIN_APPROVED : ReturnCaseState.ADMIN_REJECTED;
        return apply(actor, entry, key, type, note, next, null, note, null, true);
    }

    @Transactional
    public ReturnCaseModels.Detail withdraw(Actor actor, UUID caseId, String key) {
        requireRole(actor, "customer");
        ReturnCaseRepository.Case entry = lockAccessible(actor, caseId);
        ReturnCaseModels.Detail replay = replayIfPresent(actor, entry, key, "WITHDRAW", "");
        if (replay != null) return replay;
        if (entry.status() != ReturnCaseState.VENDOR_PENDING && entry.status() != ReturnCaseState.VENDOR_REJECTED) {
            throw conflict("Case cannot be withdrawn");
        }
        return apply(actor, entry, key, "WITHDRAW", "", ReturnCaseState.WITHDRAWN,
                null, null, null, true);
    }

    private ReturnCaseModels.Detail apply(Actor actor, ReturnCaseRepository.Case entry, String key, String type,
                                          String note, ReturnCaseState next, String vendorResponse,
                                          String adminResponse, OffsetDateTime appealDeadline, boolean closed) {
        Integer settlementAmount = null;
        if (next == ReturnCaseState.VENDOR_APPROVED || next == ReturnCaseState.ADMIN_APPROVED) {
            // Lock the purchased item to serialize approvals of separate cases.
            ReturnCaseRepository.Item item = repository.lockOwnedItem(entry.orderItemId(), entry.customerUserId());
            if (item == null) throw conflict("Purchased item is unavailable");
            int priorApproved = repository.approvedQuantity(entry.orderItemId());
            if (priorApproved > item.quantity() - entry.quantity()) {
                throw conflict("Approved quantity exceeds purchased quantity");
            }
            long previousNet = (long) item.unitPrice() * priorApproved * item.storeNet() / item.storeSubtotal();
            long cumulativeNet = (long) item.unitPrice() * (priorApproved + entry.quantity())
                    * item.storeNet() / item.storeSubtotal();
            settlementAmount = Math.toIntExact(cumulativeNet - previousNet);
        }
        if (repository.transition(entry.id(), entry.status(), next, vendorResponse, adminResponse,
                appealDeadline, closed) != 1) {
            throw conflict("Return case changed during decision");
        }
        String hash = actionHash(entry.id(), type, note);
        if (!repository.appendAction(entry.id(), actor.userId(), actor.role(), key, type,
                entry.status(), next, hash, note)) {
            throw conflict("Action key already used");
        }
        if (next == ReturnCaseState.VENDOR_APPROVED || next == ReturnCaseState.ADMIN_APPROVED) {
            repository.setApprovedAmount(entry.id(), settlementAmount);
            repository.createSettlementDecision(repository.findCase(entry.id()), actor.role(), actor.userId());
        }
        return detail(repository.findCase(entry.id()));
    }

    private ReturnCaseModels.Detail replayIfPresent(Actor actor, ReturnCaseRepository.Case entry,
                                                    String key, String type, String note) {
        requireKey(key);
        ReturnCaseRepository.StoredAction action = repository.findActionByKey(actor.userId(), key);
        if (action == null) return null;
        if (!action.caseId().equals(entry.id()) || !action.actionType().equals(type)
                || !action.payloadHash().equals(actionHash(entry.id(), type, note))) {
            throw conflict("Idempotency key has a different action");
        }
        return detail(entry);
    }

    private ReturnCaseRepository.Case lockAccessible(Actor actor, UUID caseId) {
        ReturnCaseRepository.Case entry = repository.lockCase(caseId);
        requireAccess(actor, entry);
        return entry;
    }

    private void requireAccess(Actor actor, ReturnCaseRepository.Case entry) {
        requireKnownRole(actor);
        if (entry == null) throw notFound();
        boolean allowed = switch (actor.role()) {
            case "customer" -> entry.customerUserId() == actor.userId();
            case "vendor" -> entry.vendorUserId() == actor.userId();
            case "admin" -> true;
            default -> false;
        };
        if (!allowed) throw notFound();
    }

    private void requireKnownRole(Actor actor) {
        if (actor == null || actor.userId() <= 0 || actor.role() == null
                || !(actor.role().equals("customer") || actor.role().equals("vendor") || actor.role().equals("admin"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Return case role is not supported");
        }
    }

    private void requireRole(Actor actor, String role) {
        requireKnownRole(actor);
        if (!role.equals(actor.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role cannot perform this action");
    }

    private ReturnCaseModels.Summary summary(ReturnCaseRepository.Case entry) {
        return new ReturnCaseModels.Summary(entry.id(), entry.orderItemId(), entry.orderId(), entry.quantity(),
                entry.grossAmount(), entry.approvedAmount(), entry.currency(), entry.status(),
                entry.vendorDeadline(), entry.appealDeadline());
    }

    private ReturnCaseModels.Detail detail(ReturnCaseRepository.Case entry) {
        return new ReturnCaseModels.Detail(summary(entry), entry.reason(), entry.vendorResponse(),
                entry.adminResponse(), repository.actions(entry.id()), repository.settlement(entry.id()));
    }

    private String requireNote(String note) {
        if (note == null || note.isBlank() || note.length() > 4000) throw badRequest("A response or reason is required");
        return note.trim();
    }

    private void requireKey(String key) {
        if (key == null || key.length() < 8 || key.length() > 128 || !key.matches("[A-Za-z0-9._:-]+")) {
            throw badRequest("Idempotency-Key must be 8-128 safe characters");
        }
    }

    private String actionHash(UUID caseId, String type, String note) {
        return digest(caseId + "|" + type + "|" + note);
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Return case not found");
    }
}
