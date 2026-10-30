package com.clothingstore.shop.productfilter;

import com.clothingstore.shop.dto.response.product.PaginatedResponse;
import com.clothingstore.shop.dto.response.product.ProductSummaryV2ResponseDTO;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import static com.clothingstore.shop.jooq.Tables.PRODUCT;
import static com.clothingstore.shop.jooq.Tables.PRODUCT_VARIANT;
import static com.clothingstore.shop.jooq.Tables.USERS;
import static com.clothingstore.shop.jooq.Tables.VENDOR;

@Repository
public class ProductFilterRepository {
    private static final Table<?> ATTRIBUTE = DSL.table(DSL.name("product_filter_attribute"));
    private static final Field<Integer> ATTRIBUTE_PRODUCT_ID = DSL.field(DSL.name("product_filter_attribute", "product_id"), Integer.class);
    private static final Field<String> ATTRIBUTE_KEY = DSL.field(DSL.name("product_filter_attribute", "attribute_key"), String.class);
    private static final Field<String> ATTRIBUTE_VALUE = DSL.field(DSL.name("product_filter_attribute", "attribute_value"), String.class);
    private final DSLContext dsl;

    public ProductFilterRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public PaginatedResponse<ProductSummaryV2ResponseDTO> search(ProductFilterPlan plan) {
        int total = dsl.selectCount()
                .from(PRODUCT)
                .join(VENDOR).on(PRODUCT.FK_VENDOR_ID.eq(VENDOR.VENDOR_ID))
                .join(USERS).on(VENDOR.FK_USER_ID.eq(USERS.USER_ID))
                .where(plan.condition())
                .fetchOne(0, int.class);
        var products = dsl.select(
                        PRODUCT.PRODUCT_ID,
                        PRODUCT.NAME,
                        PRODUCT.TOTAL_SALES,
                        PRODUCT.RATE,
                        PRODUCT.IMAGE_URL,
                        PRODUCT.CATEGORY,
                        USERS.ACCOUNT.as("storeName"),
                        USERS.PROFILE_PIC_URL.as("storeImageUrl"),
                        DSL.min(PRODUCT_VARIANT.PRICE).as("minPrice"),
                        DSL.max(PRODUCT_VARIANT.PRICE).as("maxPrice"),
                        PRODUCT.IS_LIST.as("isActive"))
                .from(PRODUCT)
                .join(VENDOR).on(PRODUCT.FK_VENDOR_ID.eq(VENDOR.VENDOR_ID))
                .join(USERS).on(VENDOR.FK_USER_ID.eq(USERS.USER_ID))
                .leftJoin(PRODUCT_VARIANT).on(PRODUCT.PRODUCT_ID.eq(PRODUCT_VARIANT.FK_PRODUCT_ID)
                        .and(plan.matchedVariantCondition()))
                .where(plan.condition())
                .groupBy(PRODUCT.PRODUCT_ID, PRODUCT.NAME, PRODUCT.TOTAL_SALES,
                        PRODUCT.RATE, PRODUCT.IMAGE_URL, PRODUCT.CATEGORY, PRODUCT.IS_LIST,
                        USERS.ACCOUNT, USERS.PROFILE_PIC_URL)
                .orderBy(plan.sortFields())
                .limit(plan.pageSize())
                .offset(plan.offset())
                .fetchInto(ProductSummaryV2ResponseDTO.class);
        return new PaginatedResponse<>(products, total);
    }

    public String categoryOwnedBy(int productId, int userId) {
        Record1<String> record = dsl.select(PRODUCT.CATEGORY)
                .from(PRODUCT)
                .join(VENDOR).on(PRODUCT.FK_VENDOR_ID.eq(VENDOR.VENDOR_ID))
                .join(USERS).on(VENDOR.FK_USER_ID.eq(USERS.USER_ID))
                .where(PRODUCT.PRODUCT_ID.eq(productId))
                .and(USERS.USER_ID.eq(userId))
                .and(USERS.USER_TYPE.eq("vendor"))
                .and(USERS.IS_ACTIVE.isTrue())
                .fetchOne();
        return record == null ? null : record.value1();
    }

    public void upsertAttribute(int productId, String key, String value) {
        dsl.insertInto(ATTRIBUTE)
                .columns(ATTRIBUTE_PRODUCT_ID, ATTRIBUTE_KEY, ATTRIBUTE_VALUE)
                .values(productId, key, value)
                .onConflict(ATTRIBUTE_PRODUCT_ID, ATTRIBUTE_KEY)
                .doUpdate()
                .set(ATTRIBUTE_VALUE, value)
                .execute();
    }

    public void deleteAttribute(int productId, String key) {
        dsl.deleteFrom(ATTRIBUTE)
                .where(ATTRIBUTE_PRODUCT_ID.eq(productId).and(ATTRIBUTE_KEY.eq(key)))
                .execute();
    }
}
