package com.clothingstore.shop.service;

import com.clothingstore.shop.exceptions.StoreOrderTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class StoreOrderTransitionPolicy {
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "Pending", Set.of("Shipped", "Cancelled"));

    public String canonicalTarget(String requested) {
        if (requested == null) {
            throw new StoreOrderTransitionException(HttpStatus.BAD_REQUEST, "targetStatus is required");
        }
        String value = requested.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "shipped" -> "Shipped";
            case "cancelled" -> "Cancelled";
            default -> throw new StoreOrderTransitionException(HttpStatus.BAD_REQUEST,
                    "targetStatus must be Shipped or Cancelled");
        };
    }

    public void assertAllowed(String current, String target) {
        if (!allowedTargets(current).contains(target)) {
            throw new StoreOrderTransitionException(HttpStatus.CONFLICT,
                    "Transition from " + current + " to " + target + " is not allowed");
        }
    }

    public List<String> allowedTargets(String current) {
        if (current == null) {
            return List.of();
        }
        String normalized = current.equalsIgnoreCase("Pending")
                ? "Pending" : current;
        return ALLOWED.getOrDefault(normalized, Set.of()).stream().sorted().toList();
    }
}
