package com.clothingstore.shop.dto.others.discount;

public abstract class DiscountDetailsDTO {
    private String code;
    private Integer ratio;
    private Integer amount;
    private Integer minimumSpend;
    // Getters and Setters
    public Integer getRatio() {
        return ratio;
    }

    public void setRatio(Integer ratio) {
        this.ratio = ratio;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public Integer getMinimumSpend() {
        return minimumSpend;
    }

    public void setMinimumSpend(Integer minimumSpend) {
        this.minimumSpend = minimumSpend;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

/*
export interface DiscountBaseModel {
  ratio ?: number;  // 百分比折扣
  amount ?: number; // 固定金額折扣
  minimum_spend : number;
}
 */