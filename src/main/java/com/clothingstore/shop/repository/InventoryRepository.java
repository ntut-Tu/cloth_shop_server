package com.clothingstore.shop.repository;

import com.clothingstore.shop.exceptions.SharedException;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static com.clothingstore.shop.jooq.Tables.PRODUCT_VARIANT;

@Repository
public class InventoryRepository {

    private final DSLContext dsl;

    public InventoryRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void reserveStock(Integer productVariantId, Integer quantity) throws SharedException {
        int updatedRows = dsl.update(PRODUCT_VARIANT)
                .set(PRODUCT_VARIANT.RESERVED_STOCK, PRODUCT_VARIANT.RESERVED_STOCK.plus(quantity))
                .set(PRODUCT_VARIANT.STOCK, PRODUCT_VARIANT.STOCK.minus(quantity))
                .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(productVariantId))
                .and(PRODUCT_VARIANT.STOCK.greaterOrEqual(quantity))
                .execute();

        if (updatedRows == 0) {
            throw new SharedException("Insufficient stock for product variant ID: " + productVariantId);
        }
    }

    public void finalizeStock(Integer productVariantId, Integer quantity) {
        dsl.update(PRODUCT_VARIANT)
                .set(PRODUCT_VARIANT.RESERVED_STOCK, PRODUCT_VARIANT.RESERVED_STOCK.minus(quantity))
                .execute();
    }

    public void rollbackStock(Integer productVariantId, Integer quantity) {
        dsl.update(PRODUCT_VARIANT)
                .set(PRODUCT_VARIANT.RESERVED_STOCK, PRODUCT_VARIANT.RESERVED_STOCK.minus(quantity))
                .set(PRODUCT_VARIANT.STOCK, PRODUCT_VARIANT.STOCK.plus(quantity))
                .execute();
    }
    public Integer getUnitPrice(Integer productVariantId) {
        return dsl.select(PRODUCT_VARIANT.PRICE)
                .from(PRODUCT_VARIANT)
                .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(productVariantId))
                .fetchOneInto(Integer.class);
    }

    public void checkStock(Integer productVariantId, Integer quantity) throws SharedException {
        Integer stock = dsl.select(PRODUCT_VARIANT.STOCK)
                .from(PRODUCT_VARIANT)
                .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(productVariantId))
                .fetchOneInto(Integer.class);
        if (stock == null || stock < quantity) {
            throw new SharedException("Insufficient stock for product variant ID: " + productVariantId);
        }
    }
}
