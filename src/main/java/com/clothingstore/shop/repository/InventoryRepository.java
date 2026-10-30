package com.clothingstore.shop.repository;

import com.clothingstore.shop.exceptions.SharedException;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import static com.clothingstore.shop.jooq.Tables.PRODUCT;
import static com.clothingstore.shop.jooq.Tables.PRODUCT_VARIANT;

@Repository
public class InventoryRepository {

    private final DSLContext dsl;

    public InventoryRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void reserveStock(DSLContext transaction, Integer productVariantId, Integer quantity) throws SharedException {
        if (quantity == null || quantity <= 0) {
            throw new SharedException("Reservation quantity must be positive");
        }
        int updatedRows = transaction.update(PRODUCT_VARIANT)
                .set(PRODUCT_VARIANT.RESERVED_STOCK, DSL.coalesce(PRODUCT_VARIANT.RESERVED_STOCK, 0).plus(quantity))
                .set(PRODUCT_VARIANT.STOCK, PRODUCT_VARIANT.STOCK.minus(quantity))
                .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(productVariantId))
                .and(PRODUCT_VARIANT.STOCK.greaterOrEqual(quantity))
                .execute();
        if (updatedRows == 0) {
            throw new SharedException("Insufficient stock for product variant ID: " + productVariantId);
        }
    }

    public void finalizeStock(DSLContext transaction, Integer productVariantId, Integer quantity) throws SharedException {
        int updatedRows = transaction.update(PRODUCT_VARIANT)
                .set(PRODUCT_VARIANT.RESERVED_STOCK, PRODUCT_VARIANT.RESERVED_STOCK.minus(quantity))
                .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(productVariantId))
                .and(PRODUCT_VARIANT.RESERVED_STOCK.greaterOrEqual(quantity))
                .execute();
        if (updatedRows != 1) {
            throw new SharedException("Reserved stock is inconsistent for product variant ID: " + productVariantId);
        }
    }

    public void rollbackStock(DSLContext transaction, Integer productVariantId, Integer quantity) throws SharedException {
        int updatedRows = transaction.update(PRODUCT_VARIANT)
                .set(PRODUCT_VARIANT.RESERVED_STOCK, PRODUCT_VARIANT.RESERVED_STOCK.minus(quantity))
                .set(PRODUCT_VARIANT.STOCK, PRODUCT_VARIANT.STOCK.plus(quantity))
                .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(productVariantId))
                .and(PRODUCT_VARIANT.RESERVED_STOCK.greaterOrEqual(quantity))
                .execute();
        if (updatedRows != 1) {
            throw new SharedException("Reserved stock is inconsistent for product variant ID: " + productVariantId);
        }
    }
    public Integer getUnitPrice(Integer productVariantId) {
        return dsl.select(PRODUCT_VARIANT.PRICE)
                .from(PRODUCT_VARIANT)
                .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(productVariantId))
                .fetchOneInto(Integer.class);
    }

    public Integer getUnitPriceForStore(Integer productVariantId, Integer storeId) throws SharedException {
        Integer price = dsl.select(PRODUCT_VARIANT.PRICE)
                .from(PRODUCT_VARIANT)
                .join(PRODUCT).on(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(PRODUCT.PRODUCT_ID))
                .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(productVariantId))
                .and(PRODUCT.FK_VENDOR_ID.eq(storeId))
                .and(PRODUCT.IS_LIST.isTrue())
                .fetchOneInto(Integer.class);
        if (price == null || price < 0) {
            throw new SharedException("Product variant is unavailable for store ID: " + storeId);
        }
        return price;
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
