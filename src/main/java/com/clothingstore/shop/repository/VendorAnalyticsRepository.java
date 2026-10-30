package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.response.analytics.VendorDailySalesResponseDTO;
import com.clothingstore.shop.enums.PayStatus;
import com.clothingstore.shop.enums.ShipStatus;
import com.clothingstore.shop.enums.StoreOrderStatus;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static com.clothingstore.shop.jooq.Tables.ORDER;
import static com.clothingstore.shop.jooq.Tables.STORE_ORDER;
import static com.clothingstore.shop.jooq.Tables.USERS;
import static com.clothingstore.shop.jooq.Tables.VENDOR;

@Repository
public class VendorAnalyticsRepository {
    private static final ZoneId SALES_ZONE = ZoneId.of("Asia/Taipei");

    private final DSLContext dsl;

    public VendorAnalyticsRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Integer findActiveVendorId(Integer userId) {
        return dsl.select(VENDOR.VENDOR_ID)
                .from(VENDOR)
                .join(USERS).on(VENDOR.FK_USER_ID.eq(USERS.USER_ID))
                .where(USERS.USER_ID.eq(userId))
                .and(USERS.USER_TYPE.eq("vendor"))
                .and(USERS.IS_ACTIVE.isTrue())
                .fetchOne(VENDOR.VENDOR_ID);
    }

    public List<VendorDailySalesResponseDTO> findDailySales(Integer vendorId, LocalDate fromDate, LocalDate toDate) {
        Field<LocalDate> salesDate = DSL.field(
                "({0} AT TIME ZONE 'Asia/Taipei')::date", LocalDate.class, ORDER.ORDER_DATE);
        Field<BigDecimal> grossSales = DSL.sum(STORE_ORDER.STORE_SUBTOTAL_AMOUNT.cast(BigDecimal.class));
        Field<Long> orderCount = DSL.count(STORE_ORDER.STORE_ORDER_ID).cast(Long.class);

        Condition filter = STORE_ORDER.FK_VENDOR_ID.eq(vendorId)
                .and(ORDER.PAY_STATUS.eq(PayStatus.PAID.toString()))
                .and(ORDER.SHIP_STATUS.eq(ShipStatus.COMPLETED.getStatus()))
                .and(DSL.lower(STORE_ORDER.STORE_ORDER_STATUS)
                        .ne(StoreOrderStatus.CANCELLED.getStatus().toLowerCase()))
                .and(ORDER.ORDER_DATE.isNotNull());
        if (fromDate != null) {
            OffsetDateTime fromInclusive = fromDate.atStartOfDay(SALES_ZONE).toOffsetDateTime();
            filter = filter.and(ORDER.ORDER_DATE.ge(fromInclusive));
        }
        if (toDate != null && !toDate.equals(LocalDate.MAX)) {
            OffsetDateTime toExclusive = toDate.plusDays(1).atStartOfDay(SALES_ZONE).toOffsetDateTime();
            filter = filter.and(ORDER.ORDER_DATE.lt(toExclusive));
        }

        return dsl.select(salesDate.as("date"), grossSales.as("grossSales"), orderCount.as("orderCount"))
                .from(STORE_ORDER)
                .join(ORDER).on(STORE_ORDER.FK_ORDER_ID.eq(ORDER.ORDER_ID))
                .where(filter)
                .groupBy(salesDate)
                .orderBy(salesDate.asc())
                .fetch(record -> new VendorDailySalesResponseDTO(
                        record.value1(), record.value2(), record.value3()));
    }
}
