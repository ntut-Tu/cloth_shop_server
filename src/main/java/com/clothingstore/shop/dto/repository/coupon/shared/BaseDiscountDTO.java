package com.clothingstore.shop.dto.repository.coupon.shared;

import java.math.BigDecimal;

public abstract class BaseDiscountDTO {
    private String code;
    private String startDate;
    private String endDate;
    private BigDecimal ratio;
    private Integer amount;
    private Integer minimumSpend;

    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public BigDecimal getRatio() { return ratio; }
    public void setRatio(BigDecimal ratio) { this.ratio = ratio; }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public Integer getMinimumSpend() { return minimumSpend; }
    public void setMinimumSpend(Integer minimumSpend) { this.minimumSpend = minimumSpend; }
}
