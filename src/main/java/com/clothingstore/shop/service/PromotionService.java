package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.others.tempOrder.TemporaryOrder;
import com.clothingstore.shop.promotion.*;
import com.clothingstore.shop.repository.PromotionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Locale;

@Service
public class PromotionService {
    private final PromotionRepository repository;
    private final PromotionRuleEngine engine;
    private final JwtService jwtService;
    private final ObjectMapper mapper;

    public PromotionService(PromotionRepository repository, PromotionRuleEngine engine,
                            JwtService jwtService, ObjectMapper mapper) {
        this.repository = repository;
        this.engine = engine;
        this.jwtService = jwtService;
        this.mapper = mapper;
    }

    public long create(PromotionCampaignRequest request, String token) {
        requireAdmin(token);
        if (request == null || request.code() == null) {
            throw new IllegalArgumentException("Promotion campaign is required");
        }
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9_-]{4,64}")) {
            throw new IllegalArgumentException("Promotion code must be 4-64 letters, digits, underscores or hyphens");
        }
        validateWindowAndCaps(request);
        validateDefinition(request.rule(), request.benefit());
        PromotionCampaignRequest normalized = new PromotionCampaignRequest(code, request.startsAt(),
                request.endsAt(), request.maxUses(), request.maxUsesPerCustomer(),
                request.rule(), request.benefit());
        return repository.create(normalized, encode(request.rule()), encode(request.benefit()));
    }

    public int addRevision(long id, PromotionRevisionRequest request, String token) {
        requireAdmin(token);
        if (request == null || request.expectedLatestVersion() == null
                || request.expectedLatestVersion() < 1) {
            throw new IllegalArgumentException("Expected latest version is required");
        }
        validateDefinition(request.rule(), request.benefit());
        return repository.addRevision(id, request.expectedLatestVersion(),
                encode(request.rule()), encode(request.benefit()));
    }

    public PromotionCampaign publish(long id, PromotionPublishRequest request, String token) {
        requireAdmin(token);
        if (request == null || request.expectedLatestVersion() == null || request.version() == null) {
            throw new IllegalArgumentException("Publish version is required");
        }
        return repository.publish(id, request.expectedLatestVersion(), request.version());
    }

    public PromotionCampaign pause(long id, String token) {
        requireAdmin(token);
        return repository.pause(id);
    }

    public PromotionCampaign get(long id, String token) {
        requireAdmin(token);
        return repository.findById(id);
    }

    public PromotionVersion getVersion(long id, int version, String token) {
        requireAdmin(token);
        PromotionRepository.StoredVersion stored = repository.findVersion(id, version);
        return new PromotionVersion(id, version,
                decode(stored.ruleJson(), PromotionRule.class),
                decode(stored.benefitJson(), PromotionBenefit.class));
    }

    /**
     * Applies an immutable published version to the already priced temporary
     * order. The redemption cap is checked again when the order is committed.
     */
    public void applyToOrder(TemporaryOrder order, String code) {
        if (code == null || code.isBlank()) return;
        try {
            applyToOrderWithCode(order, code);
        } catch (IllegalArgumentException error) {
            throw new PromotionNotApplicableException(error.getMessage(), error);
        }
    }

    private void applyToOrderWithCode(TemporaryOrder order, String code) {
        PromotionRepository.ActiveVersion active = repository.findActiveVersion(
                code.trim().toUpperCase(Locale.ROOT), OffsetDateTime.now());
        if (active.maxUses() != null && active.useCount() >= active.maxUses()) {
            throw new IllegalArgumentException("Promotion usage limit reached");
        }
        int customerId = repository.customerIdForUser(order.getCustomerId());
        if (active.maxUsesPerCustomer() != null
                && repository.customerRedemptions(active.campaignId(), customerId)
                >= active.maxUsesPerCustomer()) {
            throw new IllegalArgumentException("Customer promotion usage limit reached");
        }
        PromotionRule rule = decode(active.ruleJson(), PromotionRule.class);
        PromotionBenefit benefit = decode(active.benefitJson(), PromotionBenefit.class);
        PromotionCart cart = repository.loadCart(order, customerId);
        if (!engine.compile(rule).test(cart)) {
            throw new IllegalArgumentException("Cart does not meet promotion conditions");
        }
        long merchandiseNet = (long) order.getTotalAmount() - order.getShippingFee();
        int discount = engine.discount(benefit, merchandiseNet);
        if (discount <= 0) {
            throw new IllegalArgumentException("Promotion has no applicable discount");
        }
        order.setPromotionCampaignId(active.campaignId());
        order.setPromotionVersion(active.version());
        order.setPromotionDiscountAmount(discount);
        order.setTotalAmount(Math.subtractExact(order.getTotalAmount(), discount));
    }

    private void validateWindowAndCaps(PromotionCampaignRequest request) {
        if (request.startsAt() != null && request.endsAt() != null
                && !request.endsAt().isAfter(request.startsAt())) {
            throw new IllegalArgumentException("Promotion end must follow start");
        }
        if (request.maxUses() != null && request.maxUses() <= 0) {
            throw new IllegalArgumentException("maxUses must be positive");
        }
        if (request.maxUsesPerCustomer() != null && request.maxUsesPerCustomer() <= 0) {
            throw new IllegalArgumentException("maxUsesPerCustomer must be positive");
        }
    }

    private void validateDefinition(PromotionRule rule, PromotionBenefit benefit) {
        engine.compile(rule);
        engine.discount(benefit, Integer.MAX_VALUE);
    }

    private void requireAdmin(String token) {
        try {
            if (token == null || !"admin".equalsIgnoreCase(jwtService.extractRole(token))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required");
            }
        } catch (io.jsonwebtoken.JwtException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid token", e);
        }
    }

    private String encode(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid promotion definition", e);
        }
    }

    private <T> T decode(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored promotion definition is invalid", e);
        }
    }
}
