package com.clothingstore.shop.returns;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class ReturnCaseRepository {
    private final DSLContext dsl;

    public ReturnCaseRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public record Item(int itemId, int orderId, int quantity, int unitPrice, int storeSubtotal,
                       int storeNet, int vendorUserId, String payStatus, String shipStatus) {}

    public record Case(UUID id, int orderItemId, int orderId, int customerUserId, int vendorUserId,
                       int quantity, int grossAmount, int approvedAmount, String currency,
                       ReturnCaseState status, String reason, String vendorResponse, String adminResponse,
                       String createKey, String requestHash, OffsetDateTime vendorDeadline,
                       OffsetDateTime appealDeadline) {}

    public record StoredAction(UUID caseId, int actorUserId, String actionKey, String actionType, String payloadHash) {}

    public Item lockOwnedItem(int itemId, int customerUserId) {
        Record record = dsl.fetchOne("""
                SELECT oi.order_item_id, o.order_id, oi.quantity, oi.unit_price,
                       so.store_subtotal_amount, so.store_net_amount, v.fk_user_id AS vendor_user_id,
                       o.pay_status, o.ship_status
                FROM order_item oi
                JOIN "order" o ON o.order_id = oi.fk_order_id
                JOIN customer c ON c.customer_id = o.fk_customer_id
                JOIN store_order so ON so.store_order_id = oi.fk_store_order_id AND so.fk_order_id = o.order_id
                JOIN vendor v ON v.vendor_id = so.fk_vendor_id
                WHERE oi.order_item_id = ? AND c.fk_user_id = ? FOR UPDATE OF oi
                """, itemId, customerUserId);
        if (record == null) return null;
        return new Item(record.get("order_item_id", Integer.class), record.get("order_id", Integer.class),
                record.get("quantity", Integer.class), record.get("unit_price", Integer.class),
                record.get("store_subtotal_amount", Integer.class), record.get("store_net_amount", Integer.class),
                record.get("vendor_user_id", Integer.class), record.get("pay_status", String.class),
                record.get("ship_status", String.class));
    }

    public boolean hasLegacyRefund(int itemId) {
        return Boolean.TRUE.equals(dsl.fetchValue(
                "SELECT EXISTS (SELECT 1 FROM refund_request WHERE fk_order_item_id = ?)", itemId));
    }

    public int reservedQuantity(int itemId) {
        Record record = dsl.fetchOne("""
                SELECT COALESCE(SUM(requested_quantity), 0)::integer FROM return_case
                WHERE order_item_id = ? AND status IN (
                  'VENDOR_PENDING', 'VENDOR_REJECTED', 'ADMIN_PENDING', 'VENDOR_APPROVED', 'ADMIN_APPROVED')
                """, itemId);
        Integer quantity = record == null ? null : record.get(0, Integer.class);
        return quantity == null ? 0 : quantity;
    }

    public int approvedQuantity(int itemId) {
        Record record = dsl.fetchOne("""
                SELECT COALESCE(SUM(approved_quantity), 0)::integer FROM return_settlement_decision
                WHERE order_item_id = ?
                """, itemId);
        return record.get(0, Integer.class);
    }

    public Case findByCreateKey(int userId, String key) {
        return mapCase(dsl.fetchOne("SELECT * FROM return_case WHERE customer_user_id = ? AND create_key = ?", userId, key));
    }

    public Case findCase(UUID caseId) {
        return mapCase(dsl.fetchOne("SELECT * FROM return_case WHERE case_id = ?", caseId));
    }

    public Case lockCase(UUID caseId) {
        return mapCase(dsl.fetchOne("SELECT * FROM return_case WHERE case_id = ? FOR UPDATE", caseId));
    }

    public Case createCase(UUID caseId, Item item, int customerUserId, int quantity, int grossAmount,
                           String reason, String key, String hash, OffsetDateTime vendorDeadline) {
        return mapCase(dsl.fetchOne("""
                INSERT INTO return_case(case_id, order_item_id, order_id, customer_user_id, vendor_user_id,
                  requested_quantity, gross_amount, approved_amount, currency, status, reason,
                  create_key, request_hash, vendor_deadline)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, 'TWD', 'VENDOR_PENDING', ?, ?, ?, ?)
                ON CONFLICT DO NOTHING RETURNING *
                """, caseId, item.itemId(), item.orderId(), customerUserId, item.vendorUserId(), quantity,
                grossAmount, reason, key, hash, vendorDeadline));
    }

    public StoredAction findActionByKey(int actorUserId, String key) {
        Record record = dsl.fetchOne("""
                SELECT case_id, actor_user_id, action_key, action_type, payload_hash
                FROM return_case_action WHERE actor_user_id = ? AND action_key = ?
                """, actorUserId, key);
        return record == null ? null : new StoredAction(record.get("case_id", UUID.class),
                record.get("actor_user_id", Integer.class), record.get("action_key", String.class),
                record.get("action_type", String.class), record.get("payload_hash", String.class));
    }

    public boolean appendAction(UUID caseId, Integer actorUserId, String actorRole, String key, String type,
                                ReturnCaseState from, ReturnCaseState to, String hash, String note) {
        return dsl.fetchOne("""
                INSERT INTO return_case_action(action_id, case_id, actor_user_id, actor_role, action_key,
                  action_type, from_status, to_status, payload_hash, note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING RETURNING action_id
                """, UUID.randomUUID(), caseId, actorUserId, actorRole, key, type,
                from == null ? null : from.name(), to.name(), hash, note) != null;
    }

    public int transition(UUID caseId, ReturnCaseState expected, ReturnCaseState next,
                          String vendorResponse, String adminResponse,
                          OffsetDateTime appealDeadline, boolean closed) {
        return dsl.execute("""
                UPDATE return_case SET status = ?,
                  vendor_response = COALESCE(?, vendor_response),
                  admin_response = COALESCE(?, admin_response),
                  appeal_deadline = ?,
                  closed_at = CASE WHEN ? THEN now() ELSE closed_at END,
                  updated_at = now()
                WHERE case_id = ? AND status = ?
                """, next.name(), vendorResponse, adminResponse, appealDeadline, closed, caseId, expected.name());
    }

    public void setApprovedAmount(UUID caseId, int amount) {
        int changed = dsl.execute("""
                UPDATE return_case SET approved_amount = ?, updated_at = now()
                WHERE case_id = ? AND status IN ('VENDOR_APPROVED', 'ADMIN_APPROVED')
                """, amount, caseId);
        if (changed != 1) throw new IllegalStateException("Approved return case changed during settlement");
    }

    public void createSettlementDecision(Case entry, String source, int actorUserId) {
        dsl.execute("""
                INSERT INTO return_settlement_decision(case_id, order_id, order_item_id, approved_quantity,
                  amount, currency, decision_source, decided_by_user_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, entry.id(), entry.orderId(), entry.orderItemId(), entry.quantity(),
                entry.approvedAmount(), entry.currency(), source, actorUserId);
    }

    public List<Case> list(String role, int userId, int limit, int offset) {
        String filter = switch (role) {
            case "customer" -> "customer_user_id = ?";
            case "vendor" -> "vendor_user_id = ?";
            case "admin" -> "1 = 1";
            default -> throw new IllegalArgumentException("Invalid role");
        };
        String sql = "SELECT * FROM return_case WHERE " + filter + " ORDER BY created_at DESC, case_id DESC LIMIT ? OFFSET ?";
        return (role.equals("admin") ? dsl.fetch(sql, limit, offset) : dsl.fetch(sql, userId, limit, offset))
                .map(this::mapCase);
    }

    public List<ReturnCaseModels.Action> actions(UUID caseId) {
        return dsl.fetch("""
                SELECT action_id, actor_role, action_type, from_status, to_status, note, created_at
                FROM return_case_action WHERE case_id = ? ORDER BY created_at, action_id
                """, caseId).map(record -> new ReturnCaseModels.Action(
                record.get("action_id", UUID.class), record.get("actor_role", String.class),
                record.get("action_type", String.class),
                record.get("from_status", String.class) == null ? null : ReturnCaseState.valueOf(record.get("from_status", String.class)),
                ReturnCaseState.valueOf(record.get("to_status", String.class)),
                record.get("note", String.class), record.get("created_at", OffsetDateTime.class)));
    }

    public ReturnCaseModels.Settlement settlement(UUID caseId) {
        Record record = dsl.fetchOne("SELECT * FROM return_settlement_decision WHERE case_id = ?", caseId);
        if (record == null) return null;
        return new ReturnCaseModels.Settlement(record.get("case_id", UUID.class),
                record.get("order_id", Integer.class), record.get("order_item_id", Integer.class),
                record.get("approved_quantity", Integer.class), record.get("amount", Integer.class),
                record.get("currency", String.class), record.get("decision_source", String.class),
                record.get("created_at", OffsetDateTime.class));
    }

    public List<Case> lockExpiredBatch(int limit) {
        return dsl.fetch("""
                SELECT * FROM return_case
                WHERE (status = 'VENDOR_PENDING' AND vendor_deadline <= now())
                   OR (status = 'VENDOR_REJECTED' AND appeal_deadline <= now())
                ORDER BY COALESCE(appeal_deadline, vendor_deadline), case_id
                LIMIT ? FOR UPDATE SKIP LOCKED
                """, limit).map(this::mapCase);
    }

    private Case mapCase(Record record) {
        if (record == null) return null;
        return new Case(record.get("case_id", UUID.class), record.get("order_item_id", Integer.class),
                record.get("order_id", Integer.class), record.get("customer_user_id", Integer.class),
                record.get("vendor_user_id", Integer.class), record.get("requested_quantity", Integer.class),
                record.get("gross_amount", Integer.class), record.get("approved_amount", Integer.class),
                record.get("currency", String.class), ReturnCaseState.valueOf(record.get("status", String.class)),
                record.get("reason", String.class), record.get("vendor_response", String.class),
                record.get("admin_response", String.class), record.get("create_key", String.class),
                record.get("request_hash", String.class), record.get("vendor_deadline", OffsetDateTime.class),
                record.get("appeal_deadline", OffsetDateTime.class));
    }
}
