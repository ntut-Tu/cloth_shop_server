package com.clothingstore.shop.webhook;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class WebhookRepository {
    private final DSLContext dsl;

    public WebhookRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Integer activeVendorId(int userId) {
        Record record = dsl.fetchOne("SELECT v.vendor_id FROM vendor v JOIN users u ON u.user_id = v.fk_user_id " +
                "WHERE u.user_id = ? AND u.user_type = 'vendor' AND u.is_active = true", userId);
        return record == null ? null : record.get("vendor_id", Integer.class);
    }

    public List<SubscriptionView> subscriptions(int vendorId) {
        return dsl.fetch("SELECT subscription_id, endpoint_url, active, created_at " +
                        "FROM webhook_subscription WHERE vendor_id = ? AND deleted_at IS NULL " +
                        "ORDER BY subscription_id", vendorId)
                .map(this::subscriptionView);
    }

    public SubscriptionView subscription(int vendorId, long subscriptionId) {
        Record record = dsl.fetchOne("SELECT subscription_id, endpoint_url, active, created_at " +
                "FROM webhook_subscription WHERE vendor_id = ? AND subscription_id = ? " +
                "AND deleted_at IS NULL", vendorId, subscriptionId);
        return record == null ? null : subscriptionView(record);
    }

    public long create(int vendorId, String endpointUrl, String secret, Set<String> events) {
        Record record = dsl.fetchOne("INSERT INTO webhook_subscription(vendor_id, endpoint_url, secret) " +
                "VALUES (?, ?, ?) RETURNING subscription_id", vendorId, endpointUrl, secret);
        long id = record.get("subscription_id", Long.class);
        replaceEvents(id, events);
        return id;
    }

    public int update(int vendorId, long subscriptionId, String endpointUrl, boolean active,
                      Set<String> events) {
        int changed = dsl.execute("UPDATE webhook_subscription SET endpoint_url = ?, active = ? " +
                "WHERE subscription_id = ? AND vendor_id = ? AND deleted_at IS NULL",
                endpointUrl, active, subscriptionId, vendorId);
        if (changed == 1) {
            replaceEvents(subscriptionId, events);
            if (!active) cancelUndelivered(subscriptionId);
        }
        return changed;
    }

    public int rotateSecret(int vendorId, long subscriptionId, String secret) {
        return dsl.execute("UPDATE webhook_subscription SET secret = ? " +
                "WHERE subscription_id = ? AND vendor_id = ? AND deleted_at IS NULL",
                secret, subscriptionId, vendorId);
    }

    public int delete(int vendorId, long subscriptionId) {
        int changed = dsl.execute("UPDATE webhook_subscription SET active = false, deleted_at = now() " +
                "WHERE subscription_id = ? AND vendor_id = ? AND deleted_at IS NULL",
                subscriptionId, vendorId);
        if (changed == 1) cancelUndelivered(subscriptionId);
        return changed;
    }

    public List<DeliveryView> deliveries(int vendorId, long subscriptionId, int limit) {
        return dsl.fetch("SELECT d.delivery_id, d.event_id, e.event_type, d.status, d.attempt_count, " +
                "d.response_status, d.last_error, d.next_attempt_at, d.delivered_at " +
                "FROM webhook_delivery d JOIN webhook_outbox e ON e.event_id = d.event_id " +
                "JOIN webhook_subscription s ON s.subscription_id = d.subscription_id " +
                "WHERE s.vendor_id = ? AND s.subscription_id = ? AND s.deleted_at IS NULL " +
                "ORDER BY d.delivery_id DESC LIMIT ?", vendorId, subscriptionId, limit)
                .map(row -> new DeliveryView(row.get("delivery_id", Long.class),
                        row.get("event_id", Long.class), row.get("event_type", String.class),
                        row.get("status", String.class), row.get("attempt_count", Integer.class),
                        row.get("response_status", Integer.class), row.get("last_error", String.class),
                        row.get("next_attempt_at", OffsetDateTime.class),
                        row.get("delivered_at", OffsetDateTime.class)));
    }

    public void expireExhausted(int maxAttempts) {
        dsl.execute("UPDATE webhook_delivery SET status = 'DEAD', lease_token = NULL, lease_until = NULL, " +
                "last_error = CASE WHEN status = 'SENDING' THEN 'Delivery lease expired after final attempt' " +
                "ELSE 'Retry limit reached' END, updated_at = now() " +
                "WHERE attempt_count >= ? AND (status = 'PENDING' OR " +
                "(status = 'SENDING' AND lease_until < now()))", maxAttempts);
    }

    public List<DeliveryAttempt> claim(int batchSize, int maxAttempts, long leaseSeconds) {
        UUID leaseToken = UUID.randomUUID();
        return dsl.transactionResult(configuration -> {
            DSLContext transaction = DSL.using(configuration);
            List<Long> ids = transaction.fetch("WITH claimed AS (" +
                    "SELECT d.delivery_id FROM webhook_delivery d " +
                    "JOIN webhook_subscription s ON s.subscription_id = d.subscription_id " +
                    "WHERE d.status IN ('PENDING', 'SENDING') AND d.next_attempt_at <= now() " +
                    "AND (d.lease_until IS NULL OR d.lease_until < now()) " +
                    "AND d.attempt_count < ? AND s.active = true AND s.deleted_at IS NULL " +
                    "ORDER BY d.next_attempt_at, d.delivery_id LIMIT ? FOR UPDATE OF d SKIP LOCKED" +
                    ") UPDATE webhook_delivery d SET status = 'SENDING', attempt_count = d.attempt_count + 1, " +
                    "lease_token = ?, lease_until = now() + (? * interval '1 second'), updated_at = now() " +
                    "FROM claimed WHERE d.delivery_id = claimed.delivery_id RETURNING d.delivery_id",
                    maxAttempts, batchSize, leaseToken, leaseSeconds).getValues("delivery_id", Long.class);
            List<DeliveryAttempt> attempts = new ArrayList<>(ids.size());
            for (long id : ids) {
                Record row = transaction.fetchOne("SELECT d.delivery_id, d.attempt_count, e.event_id, " +
                        "e.event_type, e.payload::text AS payload, s.endpoint_url, s.secret " +
                        "FROM webhook_delivery d JOIN webhook_outbox e ON e.event_id = d.event_id " +
                        "JOIN webhook_subscription s ON s.subscription_id = d.subscription_id " +
                        "WHERE d.delivery_id = ?", id);
                attempts.add(new DeliveryAttempt(id, row.get("event_id", Long.class),
                        row.get("event_type", String.class), row.get("payload", String.class),
                        row.get("endpoint_url", String.class), row.get("secret", String.class),
                        row.get("attempt_count", Integer.class), leaseToken));
            }
            return attempts;
        });
    }

    public void delivered(DeliveryAttempt attempt, int responseStatus) {
        dsl.execute("UPDATE webhook_delivery SET status = 'DELIVERED', response_status = ?, " +
                "last_error = NULL, delivered_at = now(), lease_token = NULL, lease_until = NULL, updated_at = now() " +
                "WHERE delivery_id = ? AND lease_token = ? AND status = 'SENDING'",
                responseStatus, attempt.id(), attempt.leaseToken());
    }

    public void failed(DeliveryAttempt attempt, Integer responseStatus, String error,
                       OffsetDateTime nextAttemptAt, boolean exhausted) {
        dsl.execute("UPDATE webhook_delivery SET status = ?, response_status = ?, last_error = ?, " +
                "next_attempt_at = ?, lease_token = NULL, lease_until = NULL, updated_at = now() " +
                "WHERE delivery_id = ? AND lease_token = ? AND status = 'SENDING'",
                exhausted ? "DEAD" : "PENDING", responseStatus, error, nextAttemptAt,
                attempt.id(), attempt.leaseToken());
    }

    private SubscriptionView subscriptionView(Record record) {
        long id = record.get("subscription_id", Long.class);
        return new SubscriptionView(id, record.get("endpoint_url", String.class),
                new LinkedHashSet<>(dsl.fetch("SELECT event_type FROM webhook_subscription_event " +
                        "WHERE subscription_id = ? ORDER BY event_type", id)
                        .getValues("event_type", String.class)),
                Boolean.TRUE.equals(record.get("active", Boolean.class)),
                record.get("created_at", OffsetDateTime.class));
    }

    private void replaceEvents(long subscriptionId, Set<String> events) {
        dsl.execute("DELETE FROM webhook_subscription_event WHERE subscription_id = ?", subscriptionId);
        for (String event : events) {
            dsl.execute("INSERT INTO webhook_subscription_event(subscription_id, event_type) VALUES (?, ?)",
                    subscriptionId, event);
        }
    }

    private void cancelUndelivered(long subscriptionId) {
        dsl.execute("UPDATE webhook_delivery SET status = 'CANCELLED', lease_token = NULL, " +
                "lease_until = NULL, updated_at = now() WHERE subscription_id = ? " +
                "AND status IN ('PENDING', 'SENDING')", subscriptionId);
    }
}
