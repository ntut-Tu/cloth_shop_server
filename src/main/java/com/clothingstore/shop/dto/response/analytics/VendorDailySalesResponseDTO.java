package com.clothingstore.shop.dto.response.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VendorDailySalesResponseDTO {
    private final LocalDate date;
    private final BigDecimal grossSales;
    private final Long orderCount;

    public VendorDailySalesResponseDTO(LocalDate date, BigDecimal grossSales, Long orderCount) {
        this.date = date;
        this.grossSales = grossSales;
        this.orderCount = orderCount;
    }

    public LocalDate getDate() {
        return date;
    }

    public BigDecimal getGrossSales() {
        return grossSales;
    }

    public Long getOrderCount() {
        return orderCount;
    }
}
