package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.others.checkout.ReservationSnapshot;
import com.clothingstore.shop.dto.request.checkout.SubmitOrderRequestDTO;
import com.clothingstore.shop.enums.PayMethod;
import com.clothingstore.shop.enums.PayStatus;
import com.clothingstore.shop.exceptions.ReservationException;
import com.clothingstore.shop.exceptions.SharedException;
import com.clothingstore.shop.service.ReservationTransition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static com.clothingstore.shop.service.ReservationTransition.Decision;
import static com.clothingstore.shop.service.ReservationTransition.Status;

/** Owns every state transition and stock movement for a persisted checkout reservation. */
@Repository
public class CheckoutReservationRepository {
    private final DSLContext dsl;
    private final ObjectMapper mapper;
    private final InventoryRepository inventory;
    private final CheckoutRepository checkout;

    public CheckoutReservationRepository(DSLContext dsl, ObjectMapper mapper,
                                         InventoryRepository inventory, CheckoutRepository checkout) {
        this.dsl = dsl;
        this.mapper = mapper;
        this.inventory = inventory;
        this.checkout = checkout;
    }

    public String reserve(ReservationSnapshot quote) {
        String id = UUID.randomUUID().toString();
        String snapshot = toJson(quote);
        dsl.transaction(configuration -> {
            DSLContext transaction = DSL.using(configuration);
            transaction.execute("insert into checkout_reservation "
                            + "(reservation_id, owner_user_id, status, expires_at, snapshot) "
                            + "values (?, ?, 'HELD', now() + interval '15 minutes', cast(? as jsonb))",
                    id, quote.userId(), snapshot);
            for (Map.Entry<Integer, Integer> item : quote.quantitiesByVariant().entrySet()) {
                transaction.execute("insert into checkout_reservation_item "
                                + "(reservation_id, product_variant_id, quantity) values (?, ?, ?)",
                        id, item.getKey(), item.getValue());
                try {
                    inventory.reserveStock(transaction, item.getKey(), item.getValue());
                } catch (SharedException e) {
                    throw new ReservationException(HttpStatus.CONFLICT, e.getMessage());
                }
            }
        });
        return id;
    }

    public Confirmation confirm(String reservationId, int userId, SubmitOrderRequestDTO request) {
        String requestFingerprint = fingerprint(request);
        Confirmation result = dsl.transactionResult(configuration -> {
            DSLContext transaction = DSL.using(configuration);
            LockedReservation reservation = loadLocked(transaction, reservationId, userId);
            Decision decision = ReservationTransition.confirm(reservation.status(),
                    reservation.expiresAt(), reservation.databaseNow());
            if (decision == Decision.RETURN_CONFIRMED) {
                ReservationTransition.requireMatchingConfirmation(
                        reservation.confirmationFingerprint(), requestFingerprint);
                return new Confirmation(reservation.orderId(), reservation.quote(),
                        paymentStatus(request), false);
            }
            if (decision == Decision.RELEASE) {
                release(transaction, reservation.quote());
                removeHeldItems(transaction, reservationId);
                transaction.execute("update checkout_reservation set status = 'EXPIRED' where reservation_id = ?",
                        reservationId);
                return new Confirmation(null, reservation.quote(), null, true);
            }

            int orderId;
            try {
                orderId = checkout.saveOrder(transaction, request, reservation.quote());
                for (Map.Entry<Integer, Integer> item : reservation.quote().quantitiesByVariant().entrySet()) {
                    inventory.finalizeStock(transaction, item.getKey(), item.getValue());
                }
            } catch (SharedException e) {
                throw new IllegalStateException("Unable to finalize checkout reservation", e);
            }
            removeHeldItems(transaction, reservationId);
            transaction.execute("update checkout_reservation "
                            + "set status = 'CONFIRMED', order_id = ?, confirmation_fingerprint = ? "
                            + "where reservation_id = ?",
                    orderId, requestFingerprint, reservationId);
            return new Confirmation(orderId, reservation.quote(), paymentStatus(request), false);
        });
        if (result.expired()) {
            throw new ReservationException(HttpStatus.GONE, "Reservation has expired");
        }
        return result;
    }

    public void cancel(String reservationId, int userId) {
        dsl.transaction(configuration -> {
            DSLContext transaction = DSL.using(configuration);
            LockedReservation reservation = loadLocked(transaction, reservationId, userId);
            if (ReservationTransition.cancel(reservation.status()) == Decision.ALREADY_RELEASED) {
                return;
            }
            release(transaction, reservation.quote());
            removeHeldItems(transaction, reservationId);
            String status = reservation.expiresAt().isAfter(reservation.databaseNow())
                    ? "CANCELLED" : "EXPIRED";
            transaction.execute("update checkout_reservation set status = ? where reservation_id = ?",
                    status, reservationId);
        });
    }

    /** A batch is locked and released in one transaction; concurrent nodes skip its locked rows. */
    public int expireBatch() {
        return dsl.transactionResult(configuration -> {
            DSLContext transaction = DSL.using(configuration);
            List<? extends Record> expired = transaction.fetch("select reservation_id, snapshot::text as snapshot "
                    + "from checkout_reservation where status = 'HELD' and expires_at <= now() "
                    + "order by expires_at limit 100 for update skip locked");
            Map<Integer, Integer> quantities = new TreeMap<>();
            for (Record row : expired) {
                ReservationSnapshot quote = fromJson(row.get("snapshot", String.class));
                quote.quantitiesByVariant().forEach((variantId, quantity) ->
                        quantities.merge(variantId, quantity, Math::addExact));
            }
            releaseQuantities(transaction, quantities);
            for (Record row : expired) {
                removeHeldItems(transaction, row.get("reservation_id", String.class));
                transaction.execute("update checkout_reservation set status = 'EXPIRED' "
                        + "where reservation_id = ?", row.get("reservation_id", String.class));
            }
            return expired.size();
        });
    }

    private LockedReservation loadLocked(DSLContext transaction, String id, int userId) {
        Record row = transaction.fetchOne("select owner_user_id, status, expires_at, "
                + "snapshot::text as snapshot, order_id, confirmation_fingerprint, "
                + "now() as database_now from checkout_reservation "
                + "where reservation_id = ? for update", id);
        if (row == null) {
            throw new ReservationException(HttpStatus.NOT_FOUND, "Reservation not found");
        }
        ReservationTransition.requireOwner(row.get("owner_user_id", Integer.class), userId);
        return new LockedReservation(Status.valueOf(row.get("status", String.class)),
                row.get("expires_at", OffsetDateTime.class),
                row.get("database_now", OffsetDateTime.class),
                fromJson(row.get("snapshot", String.class)), row.get("order_id", Integer.class),
                row.get("confirmation_fingerprint", String.class));
    }

    private void release(DSLContext transaction, ReservationSnapshot quote) {
        releaseQuantities(transaction, quote.quantitiesByVariant());
    }

    private void removeHeldItems(DSLContext transaction, String reservationId) {
        transaction.execute("delete from checkout_reservation_item where reservation_id = ?", reservationId);
    }

    private void releaseQuantities(DSLContext transaction, Map<Integer, Integer> quantities) {
        for (Map.Entry<Integer, Integer> item : quantities.entrySet()) {
            try {
                inventory.rollbackStock(transaction, item.getKey(), item.getValue());
            } catch (SharedException e) {
                throw new IllegalStateException("Unable to release checkout reservation", e);
            }
        }
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize checkout reservation", e);
        }
    }

    private ReservationSnapshot fromJson(String json) {
        try {
            return mapper.readValue(json, ReservationSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to read checkout reservation", e);
        }
    }

    private String fingerprint(SubmitOrderRequestDTO request) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(toJson(request).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String paymentStatus(SubmitOrderRequestDTO request) {
        return PayMethod.CARD.getMethod().equals(request.getPayment_method())
                ? PayStatus.PAID.toString() : PayStatus.PENDING.toString();
    }

    private record LockedReservation(Status status, OffsetDateTime expiresAt,
                                     OffsetDateTime databaseNow, ReservationSnapshot quote,
                                     Integer orderId, String confirmationFingerprint) { }

    public record Confirmation(Integer orderId, ReservationSnapshot quote, String paymentStatus,
                               boolean expired) { }
}
