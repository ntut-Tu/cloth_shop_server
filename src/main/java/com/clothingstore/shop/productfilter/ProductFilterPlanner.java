package com.clothingstore.shop.productfilter;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

import static com.clothingstore.shop.jooq.Tables.PRODUCT;
import static com.clothingstore.shop.jooq.Tables.PRODUCT_VARIANT;
import static com.clothingstore.shop.jooq.Tables.USERS;

@Component
public class ProductFilterPlanner {
    public ProductFilterPlan plan(ValidatedFilterRequest request) {
        Condition condition = PRODUCT.IS_LIST.isTrue().and(USERS.IS_ACTIVE.isTrue());
        if (!request.category().equals("all")) {
            condition = condition.and(PRODUCT.CATEGORY.equalIgnoreCase(request.category()));
        }
        Condition variantCondition = DSL.trueCondition();
        boolean needsVariant = false;
        for (ValidatedCriterion criterion : request.criteria()) {
            switch (criterion.definition().source()) {
                case NAME -> condition = condition.and(text(PRODUCT.NAME, criterion));
                case RATE -> condition = condition.and(numeric(PRODUCT.RATE, criterion));
                case PRICE -> {
                    variantCondition = variantCondition.and(numeric(PRODUCT_VARIANT.PRICE, criterion));
                    needsVariant = true;
                }
                case COLOR -> {
                    variantCondition = variantCondition.and(text(PRODUCT_VARIANT.COLOR, criterion));
                    needsVariant = true;
                }
                case SIZE -> {
                    variantCondition = variantCondition.and(text(PRODUCT_VARIANT.SIZE, criterion));
                    needsVariant = true;
                }
                case IN_STOCK -> {
                    Condition available = availableStock();
                    if ((Boolean) criterion.values().get(0)) {
                        variantCondition = variantCondition.and(available);
                        needsVariant = true;
                    } else {
                        condition = condition.and(DSL.notExists(DSL.selectOne()
                                .from(PRODUCT_VARIANT)
                                .where(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(PRODUCT.PRODUCT_ID).and(available))));
                    }
                }
                case CUSTOM -> condition = condition.and(custom(criterion));
            }
        }
        if (needsVariant) {
            condition = condition.and(DSL.exists(DSL.selectOne()
                    .from(PRODUCT_VARIANT)
                    .where(PRODUCT_VARIANT.FK_PRODUCT_ID.eq(PRODUCT.PRODUCT_ID).and(variantCondition))));
        }
        List<SortField<?>> sortFields = switch (request.sort()) {
            case "sold" -> List.of(PRODUCT.TOTAL_SALES.desc(), PRODUCT.PRODUCT_ID.desc());
            case "rating" -> List.of(PRODUCT.RATE.desc().nullsLast(), PRODUCT.PRODUCT_ID.desc());
            case "price_asc" -> List.of(DSL.min(PRODUCT_VARIANT.PRICE).asc().nullsLast(), PRODUCT.PRODUCT_ID.desc());
            case "price_desc" -> List.of(DSL.max(PRODUCT_VARIANT.PRICE).desc().nullsLast(), PRODUCT.PRODUCT_ID.desc());
            default -> List.of(PRODUCT.PRODUCT_ID.desc());
        };
        return new ProductFilterPlan(condition, variantCondition, sortFields, request.pageSize(), request.offset());
    }

    private static Condition availableStock() {
        return PRODUCT_VARIANT.STOCK.gt(DSL.coalesce(PRODUCT_VARIANT.RESERVED_STOCK, 0));
    }

    private static Condition custom(ValidatedCriterion criterion) {
        Table<?> attribute = DSL.table(DSL.name("product_filter_attribute")).as("filter_attribute");
        Field<Integer> productId = DSL.field(DSL.name("filter_attribute", "product_id"), Integer.class);
        Field<String> key = DSL.field(DSL.name("filter_attribute", "attribute_key"), String.class);
        Field<String> value = DSL.field(DSL.name("filter_attribute", "attribute_value"), String.class);
        return DSL.exists(DSL.selectOne().from(attribute)
                .where(productId.eq(PRODUCT.PRODUCT_ID))
                .and(key.eq(criterion.definition().key()))
                .and(text(value, criterion)));
    }

    private static Condition text(Field<String> field, ValidatedCriterion criterion) {
        String value = (String) criterion.values().get(0);
        return switch (criterion.operator()) {
            case EQ -> field.equalIgnoreCase(value);
            case CONTAINS -> field.containsIgnoreCase(value);
            case IN -> DSL.lower(field).in(criterion.values().stream()
                    .map(item -> ((String) item).toLowerCase(Locale.ROOT)).toList());
            default -> throw new IllegalStateException("Unsupported text operator");
        };
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> Condition numeric(Field<T> field, ValidatedCriterion criterion) {
        T first = (T) criterion.values().get(0);
        return switch (criterion.operator()) {
            case EQ -> field.eq(first);
            case GTE -> field.ge(first);
            case LTE -> field.le(first);
            case BETWEEN -> field.between(first, (T) criterion.values().get(1));
            default -> throw new IllegalStateException("Unsupported numeric operator");
        };
    }
}
