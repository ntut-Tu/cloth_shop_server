package com.clothingstore.shop.dto.response.checkout;

import com.clothingstore.shop.dto.others.discount.CouponSummaryDTO;

public class ConfirmDiscountResponseDTO {

    private boolean isValid;
    private String discountType;
    private String reason;
    private CouponSummaryDTO coupon;

    // Getters and setters
    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public CouponSummaryDTO getCoupon() {
        return coupon;
    }

    public void setCoupon(CouponSummaryDTO coupon) {
        this.coupon = coupon;
    }
}

/*
export interface ConfirmDiscountResponseModel{
  is_valid : boolean; // 過期 || type是否屬於discount_type可以用的 || 以下架 || 已超過用戶使用次數 || 是否屬於這個店家
  discount_type ?: string;  // 若可用則會顯示是哪種優惠
  reason ?: string; //拒絕原因
  coupon ?: CouponSummaryModel;
}
export interface CouponSummaryModel {
  discount_type: 'ratio' | 'absolute' | 'gift';
  is_list: boolean;
  code: string;
  start_date: string;
  end_date: string;
  max_usage: number;
  discount_details: ShippingDiscountModel | SpecialDiscountModel | SeasonalDiscountModel;
}
export interface ShippingDiscountModel extends DiscountBaseModel {

}
export interface SpecialDiscountModel {
  buy_quantity: number;
  gift_quantity: number;
  buy_variant_id: number;
  gift_variant_id: number;
}
export interface SeasonalDiscountModel extends DiscountBaseModel {

}
export interface DiscountBaseModel {
  ratio ?: number;  // 百分比折扣
  amount ?: number; // 固定金額折扣
  minimum_spend : number;
}
 */