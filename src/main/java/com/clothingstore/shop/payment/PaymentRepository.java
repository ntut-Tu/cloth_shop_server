package com.clothingstore.shop.payment;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class PaymentRepository {
    private final DSLContext dsl;

    public PaymentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public record OrderSnapshot(int orderId, int amount, String method, String payStatus) {}

    public record Intent(UUID id, int orderId, int customerUserId, int amount,
                         String currency, PaymentState status, String createKey) {}

    public record Attempt(UUID id, UUID intentId, String idempotencyKey,
                          PaymentAttemptState status, String providerReference, String failureCode) {}

    public OrderSnapshot findOwnedOrder(int orderId, int userId) {
        Record record = dsl.fetchOne("""
                SELECT o.order_id, o.total_amount, o.payment_method, o.pay_status
                FROM "order" o JOIN customer c ON c.customer_id = o.fk_customer_id
                WHERE o.order_id = ? AND c.fk_user_id = ? FOR SHARE OF o
                """, orderId, userId);
        return record == null ? null : new OrderSnapshot(
                record.get("order_id", Integer.class), record.get("total_amount", Integer.class),
                record.get("payment_method", String.class), record.get("pay_status", String.class));
    }

    public Intent findByOrder(int orderId) {
        return mapIntent(dsl.fetchOne("SELECT * FROM payment_intent WHERE order_id = ?", orderId));
    }

    public Intent findByCustomerKey(int userId, String key) {
        return mapIntent(dsl.fetchOne("SELECT * FROM payment_intent WHERE customer_user_id = ? AND create_key = ?", userId, key));
    }

    public Intent findOwnedIntent(UUID intentId, int userId) {
        return mapIntent(dsl.fetchOne("SELECT * FROM payment_intent WHERE intent_id = ? AND customer_user_id = ?", intentId, userId));
    }

    public Intent lockIntent(UUID intentId) {
        return mapIntent(dsl.fetchOne("SELECT * FROM payment_intent WHERE intent_id = ? FOR UPDATE", intentId));
    }

    public Intent insertIntent(UUID intentId, int orderId, int userId, int amount, String key) {
        return mapIntent(dsl.fetchOne("""
                INSERT INTO payment_intent(intent_id, order_id, customer_user_id, amount, currency, status, create_key)
                VALUES (?, ?, ?, ?, 'TWD', 'REQUIRES_PAYMENT', ?)
                ON CONFLICT DO NOTHING RETURNING *
                """, intentId, orderId, userId, amount, key));
    }

    public void updateIntentState(UUID intentId, PaymentState state) {
        dsl.execute("UPDATE payment_intent SET status = ?, updated_at = now() WHERE intent_id = ?", state.name(), intentId);
    }

    public Attempt findAttempt(UUID attemptId) {
        return mapAttempt(dsl.fetchOne("SELECT * FROM payment_attempt WHERE attempt_id = ?", attemptId));
    }

    public Attempt findAttemptByKey(UUID intentId, String key) {
        return mapAttempt(dsl.fetchOne("SELECT * FROM payment_attempt WHERE intent_id = ? AND idempotency_key = ?", intentId, key));
    }

    public Attempt findLatestAttempt(UUID intentId) {
        return mapAttempt(dsl.fetchOne("""
                SELECT * FROM payment_attempt WHERE intent_id = ?
                ORDER BY created_at DESC, attempt_id DESC LIMIT 1
                """, intentId));
    }

    public Attempt insertAttempt(UUID attemptId, UUID intentId, String key) {
        return mapAttempt(dsl.fetchOne("""
                INSERT INTO payment_attempt(attempt_id, intent_id, idempotency_key, status)
                VALUES (?, ?, ?, 'PENDING')
                ON CONFLICT DO NOTHING RETURNING *
                """, attemptId, intentId, key));
    }

    public void resolveAttempt(UUID attemptId, PaymentAttemptState state, String providerReference, String failureCode) {
        dsl.execute("""
                UPDATE payment_attempt SET status = ?, provider_reference = ?, failure_code = ?, resolved_at = now()
                WHERE attempt_id = ?
                """, state.name(), providerReference, failureCode, attemptId);
    }

    public boolean insertEvent(String eventId, UUID attemptId, String eventType, String digest) {
        return dsl.fetchOne("""
                INSERT INTO payment_provider_event(event_id, attempt_id, event_type, payload_sha256)
                VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING RETURNING event_id
                """, eventId, attemptId, eventType, digest) != null;
    }

    public String findEventDigest(String eventId) {
        Record record = dsl.fetchOne("SELECT payload_sha256 FROM payment_provider_event WHERE event_id = ?", eventId);
        return record == null ? null : record.get("payload_sha256", String.class);
    }

    public int markOrderPaid(int orderId, int amount) {
        return dsl.execute("""
                UPDATE "order" SET pay_status = 'PAID'
                WHERE order_id = ? AND total_amount = ? AND pay_status = 'PENDING' AND payment_method = 'Card'
                """, orderId, amount);
    }

    private Intent mapIntent(Record record) {
        if (record == null) return null;
        return new Intent(record.get("intent_id", UUID.class), record.get("order_id", Integer.class),
                record.get("customer_user_id", Integer.class), record.get("amount", Integer.class),
                record.get("currency", String.class), PaymentState.valueOf(record.get("status", String.class)),
                record.get("create_key", String.class));
    }

    private Attempt mapAttempt(Record record) {
        if (record == null) return null;
        return new Attempt(record.get("attempt_id", UUID.class), record.get("intent_id", UUID.class),
                record.get("idempotency_key", String.class),
                PaymentAttemptState.valueOf(record.get("status", String.class)),
                record.get("provider_reference", String.class), record.get("failure_code", String.class));
    }
}
