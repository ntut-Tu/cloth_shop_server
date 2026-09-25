package com.clothingstore.shop.dto.request.checkout;

public class ConfirmDiscountRequestDTO {
    private String discount_code;
    private String type;
    private Integer store_id;

    // Getters and setters
    public String getDiscount_code() {
        return discount_code;
    }

    public void setDiscount_code(String discount_code) {
        this.discount_code = discount_code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getStore_id() {
        return store_id;
    }

    public void setStore_id(Integer store_id) {
        this.store_id = store_id;
    }

}

/*
export interface ConfirmDiscountModel {
  discount_code: string;
  type: 'order' | 'store_order'; // 明確類型，使用 union type
  store_id?: number; // 若 type 為 store_order 時才需包含
}
 */