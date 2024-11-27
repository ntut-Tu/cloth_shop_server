package com.clothingstore.shop.dto.others.checkout;

public class SpecialDiscountDTO extends DiscountDetailsDTO {
    private int buyQuantity;
    private int giftQuantity;
    private int buyVariantId;
    private int giftVariantId;

    // Getters and setters
    public int getBuyQuantity() {
        return buyQuantity;
    }

    public void setBuyQuantity(int buyQuantity) {
        this.buyQuantity = buyQuantity;
    }

    public int getGiftQuantity() {
        return giftQuantity;
    }

    public void setGiftQuantity(int giftQuantity) {
        this.giftQuantity = giftQuantity;
    }

    public int getBuyVariantId() {
        return buyVariantId;
    }

    public void setBuyVariantId(int buyVariantId) {
        this.buyVariantId = buyVariantId;
    }

    public int getGiftVariantId() {
        return giftVariantId;
    }

    public void setGiftVariantId(int giftVariantId) {
        this.giftVariantId = giftVariantId;
    }
}
