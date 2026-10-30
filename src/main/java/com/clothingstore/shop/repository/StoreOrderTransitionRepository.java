package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.response.orders.StoreOrderTransitionResponseDTO;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

import static com.clothingstore.shop.jooq.Tables.STORE_ORDER;
import static com.clothingstore.shop.jooq.Tables.USERS;
import static com.clothingstore.shop.jooq.Tables.VENDOR;

@Repository
public class StoreOrderTransitionRepository {
    private static final Table<Record> TRANSITIONS = DSL.table(DSL.name("store_order_transition"));
    private static final Field<Integer> TRANSITION_ID = DSL.field(DSL.name("transition_id"), Integer.class);
    private static final Field<Integer> STORE_ORDER_ID = DSL.field(DSL.name("store_order_id"), Integer.class);
    private static final Field<Integer> ACTOR_USER_ID = DSL.field(DSL.name("actor_user_id"), Integer.class);
    private static final Field<String> IDEMPOTENCY_KEY = DSL.field(DSL.name("idempotency_key"), String.class);
    private static final Field<String> FROM_STATUS = DSL.field(DSL.name("from_status"), String.class);
    private static final Field<String> TO_STATUS = DSL.field(DSL.name("to_status"), String.class);
    private static final Field<Integer> EXPECTED_VERSION = DSL.field(DSL.name("expected_version"), Integer.class);
    private static final Field<Integer> RESULTING_VERSION = DSL.field(DSL.name("resulting_version"), Integer.class);
    private static final Field<String> REASON = DSL.field(DSL.name("reason"), String.class);
    private static final Field<OffsetDateTime> TRANSITIONED_AT = DSL.field(DSL.name("transitioned_at"), OffsetDateTime.class);
    private static final Field<Integer> STATUS_VERSION = DSL.field(DSL.name("status_version"), Integer.class);

    private final DSLContext dsl;

    public StoreOrderTransitionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public DSLContext dsl() {
        return dsl;
    }

    public Integer findActiveVendorId(DSLContext tx, Integer userId) {
        return tx.select(VENDOR.VENDOR_ID)
                .from(VENDOR)
                .join(USERS).on(VENDOR.FK_USER_ID.eq(USERS.USER_ID))
                .where(USERS.USER_ID.eq(userId))
                .and(USERS.USER_TYPE.eq("vendor"))
                .and(USERS.IS_ACTIVE.isTrue())
                .fetchOne(VENDOR.VENDOR_ID);
    }

    public StoreOrderRow lockStoreOrder(DSLContext tx, Integer storeOrderId) {
        return tx.select(STORE_ORDER.STORE_ORDER_ID, STORE_ORDER.FK_VENDOR_ID,
                        STORE_ORDER.STORE_ORDER_STATUS, STATUS_VERSION)
                .from(STORE_ORDER)
                .where(STORE_ORDER.STORE_ORDER_ID.eq(storeOrderId))
                .forUpdate()
                .fetchOne(record -> new StoreOrderRow(record.value1(), record.value2(),
                        record.value3(), record.value4()));
    }

    public StoreOrderRow findStoreOrder(Integer storeOrderId) {
        return dsl.select(STORE_ORDER.STORE_ORDER_ID, STORE_ORDER.FK_VENDOR_ID,
                        STORE_ORDER.STORE_ORDER_STATUS, STATUS_VERSION)
                .from(STORE_ORDER)
                .where(STORE_ORDER.STORE_ORDER_ID.eq(storeOrderId))
                .fetchOne(record -> new StoreOrderRow(record.value1(), record.value2(),
                        record.value3(), record.value4()));
    }

    public StoredTransition findReplay(DSLContext tx, Integer storeOrderId, String key) {
        return tx.select(TRANSITION_ID, ACTOR_USER_ID, FROM_STATUS, TO_STATUS, EXPECTED_VERSION,
                        RESULTING_VERSION, REASON, TRANSITIONED_AT)
                .from(TRANSITIONS)
                .where(STORE_ORDER_ID.eq(storeOrderId).and(IDEMPOTENCY_KEY.eq(key)))
                .fetchOne(record -> new StoredTransition(record.value1(), record.value2(),
                        record.value3(), record.value4(), record.value5(), record.value6(),
                        record.value7(), record.value8()));
    }

    public boolean updateStatus(DSLContext tx, StoreOrderRow current, String target) {
        return tx.update(STORE_ORDER)
                .set(STORE_ORDER.STORE_ORDER_STATUS, target)
                .set(STATUS_VERSION, current.version() + 1)
                .where(STORE_ORDER.STORE_ORDER_ID.eq(current.storeOrderId()))
                .and(STATUS_VERSION.eq(current.version()))
                .execute() == 1;
    }

    public InsertedTransition insertTransition(DSLContext tx, StoreOrderRow current,
                                           Integer actorUserId, String key, String target,
                                           Integer expectedVersion, String reason) {
        return tx.insertInto(TRANSITIONS)
                .set(STORE_ORDER_ID, current.storeOrderId())
                .set(ACTOR_USER_ID, actorUserId)
                .set(IDEMPOTENCY_KEY, key)
                .set(FROM_STATUS, current.status())
                .set(TO_STATUS, target)
                .set(EXPECTED_VERSION, expectedVersion)
                .set(RESULTING_VERSION, current.version() + 1)
                .set(REASON, reason)
                .returning(TRANSITION_ID, TRANSITIONED_AT)
                .fetchOne(record -> new InsertedTransition(record.get(TRANSITION_ID),
                        record.get(TRANSITIONED_AT)));
    }

    public List<StoreOrderTransitionResponseDTO> findHistory(Integer storeOrderId,
                                                              Integer beforeTransitionId, int limit) {
        Condition condition = STORE_ORDER_ID.eq(storeOrderId);
        if (beforeTransitionId != null) {
            condition = condition.and(TRANSITION_ID.lt(beforeTransitionId));
        }
        return dsl.select(TRANSITION_ID, ACTOR_USER_ID, FROM_STATUS, TO_STATUS,
                        RESULTING_VERSION, IDEMPOTENCY_KEY, REASON, TRANSITIONED_AT)
                .from(TRANSITIONS)
                .where(condition)
                .orderBy(TRANSITION_ID.desc())
                .limit(limit)
                .fetch(record -> new StoreOrderTransitionResponseDTO(record.value1(), storeOrderId,
                        record.value2(), record.value3(), record.value4(), record.value5(),
                        record.value6(), record.value7(), false, record.value8()));
    }

    public record StoreOrderRow(Integer storeOrderId, Integer vendorId, String status, Integer version) {
    }

    public record StoredTransition(Integer transitionId, Integer actorUserId,
                                   String fromStatus, String toStatus,
                                   Integer expectedVersion, Integer resultingVersion,
                                   String reason, OffsetDateTime transitionedAt) {
    }

    public record InsertedTransition(Integer transitionId, OffsetDateTime transitionedAt) {
    }
}
