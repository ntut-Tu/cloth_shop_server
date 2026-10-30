package com.clothingstore.shop.service;

import com.clothingstore.shop.exceptions.ReservationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;

import static com.clothingstore.shop.service.ReservationTransition.Decision;
import static com.clothingstore.shop.service.ReservationTransition.Status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTransitionTest {
    private final OffsetDateTime now = OffsetDateTime.parse("2026-01-01T12:00:00Z");

    @Test
    void heldReservationCanBeConfirmedBeforeItsDeadline() {
        assertThat(ReservationTransition.confirm(Status.HELD, now.plusSeconds(1), now))
                .isEqualTo(Decision.CONFIRM);
    }

    @Test
    void confirmationAtTheDeadlineReleasesTheHold() {
        assertThat(ReservationTransition.confirm(Status.HELD, now, now))
                .isEqualTo(Decision.RELEASE);
    }

    @Test
    void alreadyConfirmedReservationReturnsTheOriginalOrder() {
        assertThat(ReservationTransition.confirm(Status.CONFIRMED, now.minusDays(1), now))
                .isEqualTo(Decision.RETURN_CONFIRMED);
    }

    @Test
    void cancelledReservationCannotBeConfirmedAgain() {
        assertThatThrownBy(() -> ReservationTransition.confirm(Status.CANCELLED, now.plusMinutes(1), now))
                .isInstanceOf(ReservationException.class)
                .satisfies(error -> assertThat(((ReservationException) error).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void expiredReservationReturnsGoneAfterBackgroundCleanup() {
        assertThatThrownBy(() -> ReservationTransition.confirm(Status.EXPIRED, now.minusMinutes(1), now))
                .isInstanceOf(ReservationException.class)
                .satisfies(error -> assertThat(((ReservationException) error).getStatus())
                        .isEqualTo(HttpStatus.GONE));
    }

    @Test
    void cancellingAHoldReleasesItOnce() {
        assertThat(ReservationTransition.cancel(Status.HELD)).isEqualTo(Decision.RELEASE);
        assertThat(ReservationTransition.cancel(Status.CANCELLED)).isEqualTo(Decision.ALREADY_RELEASED);
        assertThat(ReservationTransition.cancel(Status.EXPIRED)).isEqualTo(Decision.ALREADY_RELEASED);
    }

    @Test
    void confirmedOrdersCannotBeCancelledAsReservations() {
        assertThatThrownBy(() -> ReservationTransition.cancel(Status.CONFIRMED))
                .isInstanceOf(ReservationException.class)
                .satisfies(error -> assertThat(((ReservationException) error).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void anotherCustomerCannotUseTheReservation() {
        assertThatThrownBy(() -> ReservationTransition.requireOwner(7, 8))
                .isInstanceOf(ReservationException.class)
                .satisfies(error -> assertThat(((ReservationException) error).getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void retryWithDifferentOrderDetailsIsRejected() {
        ReservationTransition.requireMatchingConfirmation("original", "original");
        assertThatThrownBy(() -> ReservationTransition.requireMatchingConfirmation("original", "changed"))
                .isInstanceOf(ReservationException.class)
                .satisfies(error -> assertThat(((ReservationException) error).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT));
    }
}
