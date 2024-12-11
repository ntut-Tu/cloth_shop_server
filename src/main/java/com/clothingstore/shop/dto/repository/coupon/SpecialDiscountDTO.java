package com.clothingstore.shop.dto.repository.coupon;

import com.clothingstore.shop.dto.repository.coupon.shared.BaseDiscountDTO;

public class SpecialDiscountDTO extends BaseDiscountModel {
    private int specialDiscountId;
    private int buyQuantity;
    private int giftQuantity;
    private int buyVariantId;
    private int giftVariantId;

    // Getters and Setters
    public int getSpecialDiscountId() { return specialDiscountId; }
    public void setSpecialDiscountId(int specialDiscountId) { this.specialDiscountId = specialDiscountId; }
    public int getBuyQuantity() { return buyQuantity; }
    public void setBuyQuantity(int buyQuantity) { this.buyQuantity = buyQuantity; }

    public int getGiftQuantity() { return giftQuantity; }
    public void setGiftQuantity(int giftQuantity) { this.giftQuantity = giftQuantity; }

    public int getBuyVariantId() { return buyVariantId; }
    public void setBuyVariantId(int buyVariantId) { this.buyVariantId = buyVariantId; }

    public int getGiftVariantId() { return giftVariantId; }
    public void setGiftVariantId(int giftVariantId) { this.giftVariantId = giftVariantId; }
}


