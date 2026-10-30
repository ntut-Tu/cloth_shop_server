package com.clothingstore.shop.promotion;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

@Component
public class PromotionRuleEngine {
    private static final int MAX_DEPTH = 8;
    private static final int MAX_NODES = 64;

    public Predicate<PromotionCart> compile(PromotionRule rule) {
        return compile(rule, 1, new int[]{0});
    }

    private Predicate<PromotionCart> compile(PromotionRule rule, int depth, int[] count) {
        if (rule == null || rule.getType() == null || depth > MAX_DEPTH || ++count[0] > MAX_NODES) {
            throw new IllegalArgumentException("Invalid or oversized promotion rule");
        }
        String type = rule.getType().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "ALL" -> {
                List<PromotionRule> children = requireChildren(rule);
                List<Predicate<PromotionCart>> predicates = children.stream()
                        .map(child -> compile(child, depth + 1, count)).toList();
                yield cart -> predicates.stream().allMatch(predicate -> predicate.test(cart));
            }
            case "ANY" -> {
                List<PromotionRule> children = requireChildren(rule);
                List<Predicate<PromotionCart>> predicates = children.stream()
                        .map(child -> compile(child, depth + 1, count)).toList();
                yield cart -> predicates.stream().anyMatch(predicate -> predicate.test(cart));
            }
            case "NOT" -> {
                Predicate<PromotionCart> child = compile(rule.getChild(), depth + 1, count);
                yield child.negate();
            }
            case "SUBTOTAL_AT_LEAST" -> {
                int minimum = requireNonNegative(rule.getAmount(), "amount");
                yield cart -> cart.subtotal() >= minimum;
            }
            case "VENDOR_SUBTOTAL_AT_LEAST" -> {
                int vendorId = requirePositive(rule.getVendorId(), "vendorId");
                int minimum = requireNonNegative(rule.getAmount(), "amount");
                yield cart -> cart.lines().stream().filter(line -> line.vendorId() == vendorId)
                        .mapToLong(PromotionCart.Line::subtotal).sum() >= minimum;
            }
            case "ITEM_QUANTITY_AT_LEAST" -> {
                int variantId = requirePositive(rule.getVariantId(), "variantId");
                int minimum = requirePositive(rule.getQuantity(), "quantity");
                yield cart -> cart.lines().stream().filter(line -> line.variantId() == variantId)
                        .mapToLong(PromotionCart.Line::quantity).sum() >= minimum;
            }
            case "CATEGORY_QUANTITY_AT_LEAST" -> {
                String category = requireText(rule.getCategory(), "category");
                int minimum = requirePositive(rule.getQuantity(), "quantity");
                yield cart -> cart.lines().stream().filter(line -> category.equalsIgnoreCase(line.category()))
                        .mapToLong(PromotionCart.Line::quantity).sum() >= minimum;
            }
            case "CUSTOMER_ORDER_COUNT_AT_MOST" -> {
                int maximum = requireNonNegative(rule.getQuantity(), "quantity");
                yield cart -> cart.previousOrderCount() <= maximum;
            }
            default -> throw new IllegalArgumentException("Unsupported promotion rule: " + type);
        };
    }

    private List<PromotionRule> requireChildren(PromotionRule rule) {
        if (rule.getChildren() == null || rule.getChildren().isEmpty()
                || rule.getChildren().size() > MAX_NODES) {
            throw new IllegalArgumentException("Composite promotion rule requires children");
        }
        return rule.getChildren();
    }

    public int discount(PromotionBenefit benefit, long eligibleAmount) {
        if (benefit == null || benefit.getType() == null || eligibleAmount < 0
                || eligibleAmount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid promotion benefit or amount");
        }
        long discount = switch (benefit.getType().toUpperCase(Locale.ROOT)) {
            case "FIXED" -> requirePositive(benefit.getAmount(), "amount");
            case "PERCENT" -> (eligibleAmount * requirePercent(benefit.getPercent())) / 100;
            default -> throw new IllegalArgumentException("Unsupported promotion benefit");
        };
        if (benefit.getMaximumDiscount() != null) {
            discount = Math.min(discount, requirePositive(benefit.getMaximumDiscount(), "maximumDiscount"));
        }
        return Math.toIntExact(Math.min(discount, eligibleAmount));
    }

    private int requirePercent(Integer value) {
        int percent = requirePositive(value, "percent");
        if (percent > 100) throw new IllegalArgumentException("percent must be at most 100");
        return percent;
    }

    private int requirePositive(Integer value, String name) {
        if (value == null || value <= 0) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }

    private int requireNonNegative(Integer value, String name) {
        if (value == null || value < 0) throw new IllegalArgumentException(name + " must be non-negative");
        return value;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
}
