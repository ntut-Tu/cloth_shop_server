package com.clothingstore.shop.dto.request.checkout;

import com.clothingstore.shop.dto.others.checkout.CheckoutBaseOrderModel;

public class ConfirmAmountRequestDTO extends CheckoutBaseOrderModel {
    // Getters and setters
}
/*
export interface ConfirmAmountModel extends CheckoutBaseOrderModel{
}
// 基礎訂單接口
export interface CheckoutBaseOrderModel {
  order_date?: string;
  shipping_discount_code?: string;
  store_orders: CheckoutBaseStoreOrderModel[];
}

// 基礎商店訂單接口
export interface CheckoutBaseStoreOrderModel {
  tempDiscountCode?: string;
  store_id: number;
  special_discount_code?: string;
  seasonal_discount_code?: string;
  product_variants: CheckoutBaseProductVariantModel[];
}

// 基礎商品變體接口
export interface CheckoutBaseProductVariantModel {
  product_variant_id: number;
  quantity: number;
}
 */

