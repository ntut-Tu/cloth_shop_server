package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.others.tempOrder.TemporaryOrder;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryProductVariant;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryStoreOrder;
import com.clothingstore.shop.promotion.PromotionCampaign;
import com.clothingstore.shop.promotion.PromotionCampaignRequest;
import com.clothingstore.shop.promotion.PromotionCart;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PromotionRepository {
    private final DSLContext dsl;

    public PromotionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public long create(PromotionCampaignRequest request, String ruleJson, String benefitJson) {
        return dsl.transactionResult(configuration -> {
            DSLContext tx = DSL.using(configuration);
            Record campaign = tx.fetchOne("""
                    INSERT INTO promotion_campaign
                        (code, status, latest_version, starts_at, ends_at, max_uses, max_uses_per_customer)
                    VALUES (?, 'DRAFT', 1, ?, ?, ?, ?)
                    RETURNING campaign_id
                    """, request.code(), request.startsAt(), request.endsAt(),
                    request.maxUses(), request.maxUsesPerCustomer());
            long id = campaign.get("campaign_id", Long.class);
            tx.execute("""
                    INSERT INTO promotion_version (campaign_id, version, rule_json, benefit_json)
                    VALUES (?, 1, ?, ?)
                    """, id, ruleJson, benefitJson);
            return id;
        });
    }

    public int addRevision(long id, int expectedVersion, String ruleJson, String benefitJson) {
        return dsl.transactionResult(configuration -> {
            DSLContext tx = DSL.using(configuration);
            Record row = tx.fetchOne("SELECT latest_version FROM promotion_campaign WHERE campaign_id = ? FOR UPDATE", id);
            if (row == null) throw new IllegalArgumentException("Promotion campaign not found");
            int latest = row.get("latest_version", Integer.class);
            if (latest != expectedVersion) throw new IllegalStateException("Promotion version conflict");
            int next = latest + 1;
            tx.execute("INSERT INTO promotion_version (campaign_id, version, rule_json, benefit_json) VALUES (?, ?, ?, ?)",
                    id, next, ruleJson, benefitJson);
            tx.execute("UPDATE promotion_campaign SET latest_version = ? WHERE campaign_id = ?", next, id);
            return next;
        });
    }

    public PromotionCampaign publish(long id, int expectedLatestVersion, int version) {
        return dsl.transactionResult(configuration -> {
            DSLContext tx = DSL.using(configuration);
            Record row = tx.fetchOne("SELECT latest_version FROM promotion_campaign WHERE campaign_id = ? FOR UPDATE", id);
            if (row == null) throw new IllegalArgumentException("Promotion campaign not found");
            if (row.get("latest_version", Integer.class) != expectedLatestVersion) {
                throw new IllegalStateException("Promotion version conflict");
            }
            if (version < 1 || version > expectedLatestVersion
                    || tx.fetchOne("SELECT 1 FROM promotion_version WHERE campaign_id = ? AND version = ?", id, version) == null) {
                throw new IllegalArgumentException("Promotion version not found");
            }
            tx.execute("UPDATE promotion_campaign SET published_version = ?, status = 'ACTIVE' WHERE campaign_id = ?",
                    version, id);
            return findById(tx, id);
        });
    }

    public PromotionCampaign pause(long id) {
        int changed = dsl.execute("UPDATE promotion_campaign SET status = 'PAUSED' WHERE campaign_id = ?", id);
        if (changed == 0) throw new IllegalArgumentException("Promotion campaign not found");
        return findById(dsl, id);
    }

    public PromotionCampaign findById(long id) {
        return findById(dsl, id);
    }

    private PromotionCampaign findById(DSLContext context, long id) {
        Record row = context.fetchOne("SELECT * FROM promotion_campaign WHERE campaign_id = ?", id);
        if (row == null) throw new IllegalArgumentException("Promotion campaign not found");
        return new PromotionCampaign(
                row.get("campaign_id", Long.class),
                row.get("code", String.class),
                row.get("status", String.class),
                row.get("latest_version", Integer.class),
                row.get("published_version", Integer.class),
                row.get("starts_at", OffsetDateTime.class),
                row.get("ends_at", OffsetDateTime.class),
                row.get("max_uses", Integer.class),
                row.get("max_uses_per_customer", Integer.class),
                row.get("use_count", Integer.class));
    }

    public record ActiveVersion(
            long campaignId,
            int version,
            Integer maxUses,
            Integer maxUsesPerCustomer,
            int useCount,
            String ruleJson,
            String benefitJson
    ) {}

    public record StoredVersion(String ruleJson, String benefitJson) {}

    public StoredVersion findVersion(long id, int version) {
        Record row = dsl.fetchOne("""
                SELECT rule_json, benefit_json FROM promotion_version
                WHERE campaign_id = ? AND version = ?
                """, id, version);
        if (row == null) throw new IllegalArgumentException("Promotion version not found");
        return new StoredVersion(row.get("rule_json", String.class),
                row.get("benefit_json", String.class));
    }

    public ActiveVersion findActiveVersion(String code, OffsetDateTime now) {
        Record row = dsl.fetchOne("""
                SELECT c.campaign_id, c.published_version, c.max_uses, c.max_uses_per_customer,
                       c.use_count, v.rule_json, v.benefit_json
                FROM promotion_campaign c
                JOIN promotion_version v
                  ON v.campaign_id = c.campaign_id AND v.version = c.published_version
                WHERE c.code = ? AND c.status = 'ACTIVE'
                  AND (c.starts_at IS NULL OR c.starts_at <= ?)
                  AND (c.ends_at IS NULL OR c.ends_at > ?)
                """, code, now, now);
        if (row == null) throw new IllegalArgumentException("Promotion is unavailable");
        return new ActiveVersion(
                row.get("campaign_id", Long.class),
                row.get("published_version", Integer.class),
                row.get("max_uses", Integer.class),
                row.get("max_uses_per_customer", Integer.class),
                row.get("use_count", Integer.class),
                row.get("rule_json", String.class),
                row.get("benefit_json", String.class));
    }

    public int customerIdForUser(int userId) {
        Record row = dsl.fetchOne("SELECT customer_id FROM customer WHERE fk_user_id = ?", userId);
        if (row == null) throw new IllegalArgumentException("Customer not found");
        return row.get("customer_id", Integer.class);
    }

    public int priorOrderCount(int customerId) {
        return dsl.fetchOne("SELECT count(*) AS total FROM \"order\" WHERE fk_customer_id = ?", customerId)
                .get("total", Integer.class);
    }

    public int customerRedemptions(long campaignId, int customerId) {
        return dsl.fetchOne("""
                SELECT count(*) AS total FROM promotion_redemption
                WHERE campaign_id = ? AND customer_id = ?
                """, campaignId, customerId).get("total", Integer.class);
    }

    public PromotionCart loadCart(TemporaryOrder order, int customerId) {
        List<PromotionCart.Line> lines = new ArrayList<>();
        for (TemporaryStoreOrder store : order.getStoreOrders()) {
            for (TemporaryProductVariant variant : store.getProductVariants()) {
                Record row = dsl.fetchOne("""
                        SELECT p.category, p.fk_vendor_id
                        FROM product_variant pv
                        JOIN product p ON p.product_id = pv.fk_product_id
                        WHERE pv.product_variant_id = ?
                        """, variant.getProductVariantId());
                if (row == null || row.get("fk_vendor_id", Integer.class) != store.getStoreId()) {
                    throw new IllegalArgumentException("Product variant does not belong to store");
                }
                lines.add(new PromotionCart.Line(variant.getProductVariantId(),
                        store.getStoreId(), row.get("category", String.class),
                        variant.getQuantity(), variant.getUnitPrice()));
            }
        }
        return new PromotionCart(List.copyOf(lines), priorOrderCount(customerId));
    }

    /**
     * Called from the same transaction that creates the order. Locking the
     * campaign serializes global and per-customer cap checks across nodes.
     */
    public void redeem(DSLContext tx, int orderId, String quoteId, int customerId,
                       long campaignId, int version, int amount) {
        Record row = tx.fetchOne("""
                SELECT status, published_version, starts_at, ends_at, max_uses,
                       max_uses_per_customer, use_count
                FROM promotion_campaign WHERE campaign_id = ? FOR UPDATE
                """, campaignId);
        OffsetDateTime now = OffsetDateTime.now();
        if (row == null || !"ACTIVE".equals(row.get("status", String.class))
                || row.get("published_version", Integer.class) == null
                || row.get("published_version", Integer.class) != version
                || (row.get("starts_at", OffsetDateTime.class) != null
                    && row.get("starts_at", OffsetDateTime.class).isAfter(now))
                || (row.get("ends_at", OffsetDateTime.class) != null
                    && !row.get("ends_at", OffsetDateTime.class).isAfter(now))) {
            throw new IllegalStateException("Promotion is no longer active");
        }
        Integer maxUses = row.get("max_uses", Integer.class);
        if (maxUses != null && row.get("use_count", Integer.class) >= maxUses) {
            throw new IllegalStateException("Promotion usage limit reached");
        }
        Integer perCustomer = row.get("max_uses_per_customer", Integer.class);
        if (perCustomer != null && tx.fetchOne("""
                SELECT count(*) AS total FROM promotion_redemption
                WHERE campaign_id = ? AND customer_id = ?
                """, campaignId, customerId).get("total", Integer.class) >= perCustomer) {
            throw new IllegalStateException("Customer promotion usage limit reached");
        }
        tx.execute("""
                INSERT INTO promotion_redemption
                    (campaign_id, version, customer_id, order_id, quote_id, discount_amount)
                VALUES (?, ?, ?, ?, ?, ?)
                """, campaignId, version, customerId, orderId, quoteId, amount);
        tx.execute("UPDATE promotion_campaign SET use_count = use_count + 1 WHERE campaign_id = ?", campaignId);
    }
}
