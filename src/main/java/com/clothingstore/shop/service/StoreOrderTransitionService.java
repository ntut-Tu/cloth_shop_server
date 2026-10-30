package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.request.orders.StoreOrderTransitionRequestDTO;
import com.clothingstore.shop.dto.response.orders.StoreOrderTransitionResponseDTO;
import com.clothingstore.shop.dto.response.orders.StoreOrderTransitionStateDTO;
import com.clothingstore.shop.exceptions.StoreOrderTransitionException;
import com.clothingstore.shop.repository.StoreOrderTransitionRepository;
import com.clothingstore.shop.repository.StoreOrderTransitionRepository.StoreOrderRow;
import com.clothingstore.shop.repository.StoreOrderTransitionRepository.StoredTransition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class StoreOrderTransitionService {
    private final JwtService jwtService;
    private final StoreOrderTransitionPolicy policy;
    private final StoreOrderTransitionRepository repository;

    public StoreOrderTransitionService(JwtService jwtService, StoreOrderTransitionPolicy policy,
                                       StoreOrderTransitionRepository repository) {
        this.jwtService = jwtService;
        this.policy = policy;
        this.repository = repository;
    }

    public StoreOrderTransitionResponseDTO transition(String token, Integer storeOrderId,
                                                       StoreOrderTransitionRequestDTO request) {
        return transition(token, storeOrderId, request, true);
    }

    public void transitionLegacy(String token, Integer storeOrderId, String targetStatus) {
        transition(token, storeOrderId, new StoreOrderTransitionRequestDTO(
                targetStatus, "legacy-" + UUID.randomUUID(), null, null), false);
    }

    private StoreOrderTransitionResponseDTO transition(String token, Integer storeOrderId,
                                                        StoreOrderTransitionRequestDTO request,
                                                        boolean requireVersion) {
        Integer actorUserId = vendorUserId(token);
        checkOrderId(storeOrderId);
        if (request == null) {
            throw badRequest("Request body is required");
        }
        String target = policy.canonicalTarget(request.targetStatus());
        String key = normalizeKey(request.idempotencyKey());
        Integer expectedVersion = request.expectedVersion();
        if ((requireVersion && expectedVersion == null)
                || (expectedVersion != null && expectedVersion < 0)) {
            throw badRequest("expectedVersion must be a nonnegative integer");
        }
        String reason = normalizeReason(request.reason());

        return repository.dsl().transactionResult(configuration -> {
            DSLContext tx = DSL.using(configuration);
            Integer vendorId = activeVendorId(tx, actorUserId);
            StoreOrderRow current = requiredOrder(repository.lockStoreOrder(tx, storeOrderId));
            assertOwner(current, vendorId);

            StoredTransition replay = repository.findReplay(tx, storeOrderId, key);
            if (replay != null) {
                if (!Objects.equals(replay.actorUserId(), actorUserId)
                        || !Objects.equals(replay.toStatus(), target)
                        || !Objects.equals(replay.expectedVersion(), expectedVersion)
                        || !Objects.equals(replay.reason(), reason)) {
                    throw new StoreOrderTransitionException(HttpStatus.CONFLICT,
                            "Idempotency key was already used for a different transition");
                }
                return new StoreOrderTransitionResponseDTO(replay.transitionId(), storeOrderId,
                        replay.actorUserId(), replay.fromStatus(), replay.toStatus(),
                        replay.resultingVersion(), key, replay.reason(), true,
                        replay.transitionedAt());
            }
            if (expectedVersion != null && !expectedVersion.equals(current.version())) {
                throw new StoreOrderTransitionException(HttpStatus.CONFLICT,
                        "Store order version changed; refresh its transition state");
            }
            policy.assertAllowed(current.status(), target);
            if (!repository.updateStatus(tx, current, target)) {
                throw new StoreOrderTransitionException(HttpStatus.CONFLICT,
                        "Store order version changed; retry with the latest version");
            }
            var inserted = repository.insertTransition(tx, current, actorUserId, key, target,
                    expectedVersion, reason);
            return new StoreOrderTransitionResponseDTO(inserted.transitionId(), storeOrderId,
                    actorUserId, current.status(), target, current.version() + 1,
                    key, reason, false, inserted.transitionedAt());
        });
    }

    public StoreOrderTransitionStateDTO getState(String token, Integer storeOrderId) {
        Integer actorUserId = vendorUserId(token);
        checkOrderId(storeOrderId);
        Integer vendorId = activeVendorId(repository.dsl(), actorUserId);
        StoreOrderRow current = requiredOrder(repository.findStoreOrder(storeOrderId));
        assertOwner(current, vendorId);
        return new StoreOrderTransitionStateDTO(storeOrderId, current.status(), current.version(),
                policy.allowedTargets(current.status()));
    }

    public List<StoreOrderTransitionResponseDTO> getHistory(String token, Integer storeOrderId,
                                                              Integer beforeTransitionId, int limit) {
        Integer actorUserId = vendorUserId(token);
        checkOrderId(storeOrderId);
        if (limit < 1 || limit > 100 || (beforeTransitionId != null && beforeTransitionId < 1)) {
            throw badRequest("limit must be 1-100 and beforeTransitionId must be positive");
        }
        Integer vendorId = activeVendorId(repository.dsl(), actorUserId);
        StoreOrderRow current = requiredOrder(repository.findStoreOrder(storeOrderId));
        assertOwner(current, vendorId);
        return repository.findHistory(storeOrderId, beforeTransitionId, limit);
    }

    private Integer vendorUserId(String token) {
        if (token == null || token.isBlank()) {
            throw new StoreOrderTransitionException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Integer userId = jwtService.extractUserId(token);
        if (userId == null || !"vendor".equals(jwtService.extractRole(token))) {
            throw new StoreOrderTransitionException(HttpStatus.FORBIDDEN, "Vendor access required");
        }
        return userId;
    }

    private Integer activeVendorId(DSLContext tx, Integer userId) {
        Integer vendorId = repository.findActiveVendorId(tx, userId);
        if (vendorId == null) {
            throw new StoreOrderTransitionException(HttpStatus.FORBIDDEN,
                    "Active vendor access required");
        }
        return vendorId;
    }

    private void checkOrderId(Integer storeOrderId) {
        if (storeOrderId == null || storeOrderId < 1) {
            throw badRequest("storeOrderId must be positive");
        }
    }

    private StoreOrderRow requiredOrder(StoreOrderRow row) {
        if (row == null) {
            throw new StoreOrderTransitionException(HttpStatus.NOT_FOUND, "Store order not found");
        }
        return row;
    }

    private void assertOwner(StoreOrderRow row, Integer vendorId) {
        if (!vendorId.equals(row.vendorId())) {
            throw new StoreOrderTransitionException(HttpStatus.FORBIDDEN,
                    "Store order belongs to another vendor");
        }
    }

    private String normalizeKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,100}")) {
            throw badRequest("idempotencyKey must be 1-100 letters, digits, dots, underscores, colons, or hyphens");
        }
        return key;
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String normalized = reason.trim();
        if (normalized.length() > 500) {
            throw badRequest("reason must be at most 500 characters");
        }
        return normalized;
    }

    private StoreOrderTransitionException badRequest(String message) {
        return new StoreOrderTransitionException(HttpStatus.BAD_REQUEST, message);
    }
}
