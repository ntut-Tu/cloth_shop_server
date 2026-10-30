package com.clothingstore.shop.service;

import com.clothingstore.shop.exceptions.ReservationException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;

/** Decisions made while the reservation row is locked by the current transaction. */
public final class ReservationTransition {
    public enum Status { HELD, CONFIRMED, CANCELLED, EXPIRED }
    public enum Decision { CONFIRM, RETURN_CONFIRMED, RELEASE, ALREADY_RELEASED }

    private ReservationTransition() { }

    public static void requireOwner(int ownerUserId, int requesterUserId) {
        if (ownerUserId != requesterUserId) {
            throw new ReservationException(HttpStatus.FORBIDDEN,
                    "Reservation belongs to another customer");
        }
    }

    public static void requireMatchingConfirmation(String storedFingerprint,
                                                   String requestFingerprint) {
        if (!requestFingerprint.equals(storedFingerprint)) {
            throw new ReservationException(HttpStatus.CONFLICT,
                    "Reservation was confirmed with different order details");
        }
    }

    public static Decision confirm(Status status, OffsetDateTime expiresAt,
                                   OffsetDateTime now) {
        return switch (status) {
            case HELD -> expiresAt.isAfter(now) ? Decision.CONFIRM : Decision.RELEASE;
            case CONFIRMED -> Decision.RETURN_CONFIRMED;
            case CANCELLED -> throw new ReservationException(HttpStatus.CONFLICT,
                    "Reservation was cancelled");
            case EXPIRED -> throw new ReservationException(HttpStatus.GONE,
                    "Reservation has expired");
        };
    }

    public static Decision cancel(Status status) {
        return switch (status) {
            case HELD -> Decision.RELEASE;
            case CANCELLED, EXPIRED -> Decision.ALREADY_RELEASED;
            case CONFIRMED -> throw new ReservationException(HttpStatus.CONFLICT,
                    "Confirmed reservation cannot be cancelled");
        };
    }
}
