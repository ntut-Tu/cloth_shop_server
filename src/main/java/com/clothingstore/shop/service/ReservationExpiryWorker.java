package com.clothingstore.shop.service;

import com.clothingstore.shop.repository.CheckoutReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpiryWorker {
    private final CheckoutReservationRepository reservations;

    public ReservationExpiryWorker(CheckoutReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Scheduled(fixedDelayString = "${checkout.reservation.cleanup-delay-ms:60000}")
    public void releaseExpiredReservations() {
        while (reservations.expireBatch() == 100) {
            // Continue until the expired backlog is drained.
        }
    }
}
