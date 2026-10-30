package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.repository.coupon.SpecialDiscountModel;
import com.clothingstore.shop.dto.repository.coupon.StandardDiscountModel;
import com.clothingstore.shop.dto.response.coupon.DiscountDetailResponseDTO;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;

import static com.clothingstore.shop.jooq.Tables.COUPON;
import static com.clothingstore.shop.jooq.Tables.SEASONAL_DISCOUNT;
import static com.clothingstore.shop.jooq.Tables.SHIPPING_DISCOUNT;
import static com.clothingstore.shop.jooq.Tables.SPECIAL_DISCOUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CouponDetailsRepositoryTest {
    private final DSLContext records = DSL.using(SQLDialect.POSTGRES);

    @Test
    void fetchesSeasonalDiscountDetails() {
        DiscountDetailResponseDTO coupon = repositoryFor("seasonal_discount", true).fetchCouponDetails(9);

        assertEquals(9, coupon.getCouponId());
        assertEquals("SAVE10", coupon.getCode());
        StandardDiscountModel discount = assertInstanceOf(StandardDiscountModel.class, coupon.getDiscount());
        assertEquals(new BigDecimal("10.00"), discount.getRatio());
        assertEquals(100, discount.getMinimum_spend());
    }

    @Test
    void fetchesShippingDiscountDetailsIncludingShippingMethods() {
        DiscountDetailResponseDTO coupon = repositoryFor("shipping_discount", true).fetchCouponDetails(9);

        StandardDiscountModel discount = assertInstanceOf(StandardDiscountModel.class, coupon.getDiscount());
        assertEquals(50, discount.getDiscount_amount());
        assertEquals("home_delivery", discount.getApplicable_shipping_methods());
    }

    @Test
    void preservesSpecialDiscountDetails() {
        DiscountDetailResponseDTO coupon = repositoryFor("special_discount", true).fetchCouponDetails(9);

        SpecialDiscountModel discount = assertInstanceOf(SpecialDiscountModel.class, coupon.getDiscount());
        assertEquals(2, discount.getBuyQuantity());
        assertEquals(1, discount.getGiftQuantity());
        assertNull(discount.getGiftVariantId());
    }

    @Test
    void missingCouponProducesClearError() {
        assertThrows(IllegalArgumentException.class,
                () -> repositoryFor("special_discount", false).fetchCouponDetails(9));
    }

    private CouponRepository repositoryFor(String type, boolean exists) {
        MockConnection connection = new MockConnection(context -> {
            String sql = context.sql().toLowerCase(Locale.ROOT);
            if (sql.contains("special_discount")) {
                return result(specialDiscount());
            }
            if (sql.contains("seasonal_discount")) {
                return result(seasonalDiscount());
            }
            if (sql.contains("shipping_discount")) {
                return result(shippingDiscount());
            }
            if (sql.contains("coupon")) {
                return result(coupon(type, exists));
            }
            throw new IllegalStateException("Unexpected query: " + context.sql());
        });
        return new CouponRepository(DSL.using(connection, SQLDialect.POSTGRES));
    }

    private MockResult[] result(Result<Record> rows) {
        return new MockResult[]{new MockResult(rows.size(), rows)};
    }

    private Result<Record> coupon(String type, boolean exists) {
        Result<Record> rows = records.newResult(COUPON.fields());
        if (exists) {
            Record row = records.newRecord(COUPON);
            row.set(COUPON.COUPON_ID, 9);
            row.set(COUPON.CODE, "SAVE10");
            row.set(COUPON.TYPE, type);
            row.set(COUPON.IS_LIST, true);
            row.set(COUPON.START_DATE, OffsetDateTime.parse("2026-01-01T00:00:00Z"));
            row.set(COUPON.END_DATE, OffsetDateTime.parse("2026-12-31T00:00:00Z"));
            rows.add(row);
        }
        return rows;
    }

    private Result<Record> seasonalDiscount() {
        Result<Record> rows = records.newResult(SEASONAL_DISCOUNT.fields());
        Record row = records.newRecord(SEASONAL_DISCOUNT);
        row.set(SEASONAL_DISCOUNT.SEASONAL_DISCOUNT_ID, 3);
        row.set(SEASONAL_DISCOUNT.FK_COUPON_ID, 9);
        row.set(SEASONAL_DISCOUNT.DISCOUNT_TYPE, "percentage");
        row.set(SEASONAL_DISCOUNT.RATIO, new BigDecimal("10.00"));
        row.set(SEASONAL_DISCOUNT.MINIMUM_SPEND, 100);
        rows.add(row);
        return rows;
    }

    private Result<Record> shippingDiscount() {
        Result<Record> rows = records.newResult(SHIPPING_DISCOUNT.fields());
        Record row = records.newRecord(SHIPPING_DISCOUNT);
        row.set(SHIPPING_DISCOUNT.SHIPPING_DISCOUNT_ID, 4);
        row.set(SHIPPING_DISCOUNT.FK_COUPON_ID, 9);
        row.set(SHIPPING_DISCOUNT.DISCOUNT_TYPE, "fixed");
        row.set(SHIPPING_DISCOUNT.DISCOUNT_AMOUNT, 50);
        row.set(SHIPPING_DISCOUNT.APPLICABLE_SHIPPING_METHODS, "home_delivery");
        rows.add(row);
        return rows;
    }

    private Result<Record> specialDiscount() {
        Result<Record> rows = records.newResult(SPECIAL_DISCOUNT.fields());
        Record row = records.newRecord(SPECIAL_DISCOUNT);
        row.set(SPECIAL_DISCOUNT.SPECIAL_DISCOUNT_ID, 5);
        row.set(SPECIAL_DISCOUNT.FK_COUPON_ID, 9);
        row.set(SPECIAL_DISCOUNT.BUY_QUANTITY, 2);
        row.set(SPECIAL_DISCOUNT.GIFT_QUANTITY, 1);
        row.set(SPECIAL_DISCOUNT.BUY_VARIANT_ID, 7);
        rows.add(row);
        return rows;
    }
}
