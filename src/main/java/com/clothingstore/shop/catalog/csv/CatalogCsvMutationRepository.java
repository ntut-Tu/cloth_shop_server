package com.clothingstore.shop.catalog.csv;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.clothingstore.shop.jooq.Tables.PRODUCT;
import static com.clothingstore.shop.jooq.Tables.PRODUCT_VARIANT;

@Repository
public class CatalogCsvMutationRepository {
    private final DSLContext dsl;

    public CatalogCsvMutationRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public record MutationResult(Integer productId, Integer variantId) {
    }

    /** Each row is committed or rolled back independently. */
    @Transactional
    public MutationResult apply(int vendorId, CatalogCsvRow row) {
        return switch (row.operation()) {
            case ADD -> add(vendorId, row);
            case EDIT -> edit(vendorId, row);
            case DELETE -> delete(vendorId, row);
        };
    }

    private MutationResult add(int vendorId, CatalogCsvRow row) {
        Integer productId = row.productId();
        if (productId == null) {
            productId = dsl.insertInto(PRODUCT)
                    .set(PRODUCT.NAME, row.name())
                    .set(PRODUCT.DESCRIPTION, row.description())
                    .set(PRODUCT.IMAGE_URL, row.imageUrl())
                    .set(PRODUCT.CATEGORY, row.category())
                    .set(PRODUCT.IS_LIST, row.listed() == null || row.listed())
                    .set(PRODUCT.FK_VENDOR_ID, vendorId)
                    .set(PRODUCT.TOTAL_SALES, 0)
                    .set(PRODUCT.RATE, BigDecimal.ZERO)
                    .returning(PRODUCT.PRODUCT_ID)
                    .fetchOne().get(PRODUCT.PRODUCT_ID);
        } else {
            requireProduct(vendorId, productId);
        }
        Integer variantId = dsl.insertInto(PRODUCT_VARIANT)
                .set(PRODUCT_VARIANT.FK_PRODUCT_ID, productId)
                .set(PRODUCT_VARIANT.COLOR, row.color())
                .set(PRODUCT_VARIANT.SIZE, row.size())
                .set(PRODUCT_VARIANT.PRICE, row.price())
                .set(PRODUCT_VARIANT.STOCK, row.stock())
                .returning(PRODUCT_VARIANT.PRODUCT_VARIANT_ID)
                .fetchOne().get(PRODUCT_VARIANT.PRODUCT_VARIANT_ID);
        return new MutationResult(productId, variantId);
    }

    private MutationResult edit(int vendorId, CatalogCsvRow row) {
        requireProduct(vendorId, row.productId());
        if (row.variantId() != null) requireVariant(vendorId, row.productId(), row.variantId());

        boolean productChanged = row.name() != null || row.description() != null || row.imageUrl() != null
                || row.category() != null || row.listed() != null;
        if (productChanged) {
            var productUpdate = dsl.updateQuery(PRODUCT);
            productUpdate.addConditions(PRODUCT.PRODUCT_ID.eq(row.productId()), PRODUCT.FK_VENDOR_ID.eq(vendorId));
            if (row.name() != null) productUpdate.addValue(PRODUCT.NAME, row.name());
            if (row.description() != null) productUpdate.addValue(PRODUCT.DESCRIPTION, row.description());
            if (row.imageUrl() != null) productUpdate.addValue(PRODUCT.IMAGE_URL, row.imageUrl());
            if (row.category() != null) productUpdate.addValue(PRODUCT.CATEGORY, row.category());
            if (row.listed() != null) productUpdate.addValue(PRODUCT.IS_LIST, row.listed());
            productUpdate.execute();
        }

        boolean variantChanged = row.color() != null || row.size() != null || row.price() != null || row.stock() != null;
        if (variantChanged) {
            var variantUpdate = dsl.updateQuery(PRODUCT_VARIANT);
            variantUpdate.addConditions(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(row.variantId()),
                    PRODUCT_VARIANT.FK_PRODUCT_ID.eq(row.productId()));
            if (row.color() != null) variantUpdate.addValue(PRODUCT_VARIANT.COLOR, row.color());
            if (row.size() != null) variantUpdate.addValue(PRODUCT_VARIANT.SIZE, row.size());
            if (row.price() != null) variantUpdate.addValue(PRODUCT_VARIANT.PRICE, row.price());
            if (row.stock() != null) variantUpdate.addValue(PRODUCT_VARIANT.STOCK, row.stock());
            variantUpdate.execute();
        }
        return new MutationResult(row.productId(), row.variantId());
    }

    private MutationResult delete(int vendorId, CatalogCsvRow row) {
        requireProduct(vendorId, row.productId());
        if (row.variantId() == null) {
            dsl.deleteFrom(PRODUCT_VARIANT).where(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(row.productId())).execute();
            dsl.deleteFrom(PRODUCT).where(PRODUCT.PRODUCT_ID.eq(row.productId()))
                    .and(PRODUCT.FK_VENDOR_ID.eq(vendorId)).execute();
        } else {
            requireVariant(vendorId, row.productId(), row.variantId());
            dsl.deleteFrom(PRODUCT_VARIANT).where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(row.variantId()))
                    .and(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(row.productId())).execute();
        }
        return new MutationResult(row.productId(), row.variantId());
    }

    private void requireProduct(int vendorId, int productId) {
        if (dsl.select(PRODUCT.PRODUCT_ID).from(PRODUCT)
                .where(PRODUCT.PRODUCT_ID.eq(productId)).and(PRODUCT.FK_VENDOR_ID.eq(vendorId))
                .fetchOne() == null) {
            throw new IllegalArgumentException("Product not found for vendor");
        }
    }

    private void requireVariant(int vendorId, int productId, int variantId) {
        if (dsl.select(PRODUCT_VARIANT.PRODUCT_VARIANT_ID).from(PRODUCT_VARIANT)
                .join(PRODUCT).on(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(PRODUCT.PRODUCT_ID))
                .where(PRODUCT_VARIANT.PRODUCT_VARIANT_ID.eq(variantId))
                .and(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(productId))
                .and(PRODUCT.FK_VENDOR_ID.eq(vendorId)).fetchOne() == null) {
            throw new IllegalArgumentException("Variant not found for vendor product");
        }
    }

    @Transactional(readOnly = true)
    public List<CatalogCsvRow> export(int vendorId, int offset, int limit) {
        return dsl.select(PRODUCT.PRODUCT_ID, PRODUCT_VARIANT.PRODUCT_VARIANT_ID,
                        PRODUCT.NAME, PRODUCT.DESCRIPTION, PRODUCT.IMAGE_URL, PRODUCT.CATEGORY,
                        PRODUCT.IS_LIST, PRODUCT_VARIANT.COLOR, PRODUCT_VARIANT.SIZE,
                        PRODUCT_VARIANT.PRICE, PRODUCT_VARIANT.STOCK)
                .from(PRODUCT)
                .leftJoin(PRODUCT_VARIANT).on(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(PRODUCT.PRODUCT_ID))
                .where(PRODUCT.FK_VENDOR_ID.eq(vendorId))
                .orderBy(PRODUCT.PRODUCT_ID, PRODUCT_VARIANT.PRODUCT_VARIANT_ID)
                .limit(limit)
                .offset(offset)
                .fetch(this::toRow);
    }

    private CatalogCsvRow toRow(Record record) {
        return new CatalogCsvRow(CatalogCsvRow.Operation.EDIT,
                record.get(PRODUCT.PRODUCT_ID), record.get(PRODUCT_VARIANT.PRODUCT_VARIANT_ID),
                record.get(PRODUCT.NAME), record.get(PRODUCT.DESCRIPTION), record.get(PRODUCT.IMAGE_URL),
                record.get(PRODUCT.CATEGORY), record.get(PRODUCT.IS_LIST), record.get(PRODUCT_VARIANT.COLOR),
                record.get(PRODUCT_VARIANT.SIZE), record.get(PRODUCT_VARIANT.PRICE), record.get(PRODUCT_VARIANT.STOCK));
    }
}
