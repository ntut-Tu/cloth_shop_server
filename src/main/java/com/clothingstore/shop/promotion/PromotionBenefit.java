package com.clothingstore.shop.promotion;

public class PromotionBenefit {
    private String type;
    private Integer amount;
    private Integer percent;
    private Integer maximumDiscount;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public Integer getPercent() { return percent; }
    public void setPercent(Integer percent) { this.percent = percent; }
    public Integer getMaximumDiscount() { return maximumDiscount; }
    public void setMaximumDiscount(Integer maximumDiscount) { this.maximumDiscount = maximumDiscount; }
}
