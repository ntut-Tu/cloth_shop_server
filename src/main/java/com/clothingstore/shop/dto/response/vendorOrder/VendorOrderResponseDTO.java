package com.clothingstore.shop.dto.response.vendorOrder;

import com.clothingstore.shop.dto.response.vendorOrder.vendorUserOrder.VendorProductVariantDTO;
import com.clothingstore.shop.dto.response.vendorOrder.vendorUserOrder.VendorUserOrderDTO;

import java.util.List;

public class VendorOrderResponseDTO {
    private Integer storeOrderId;
    private String imageUrl;
    private String vendorCouponCode;
    private Integer totalAmount;
    private Integer totalDiscount;
    private Integer totalNetAmount;
    private List<VendorUserOrderDTO> orders;
    private String storeOrderStatus;
    private String orderPayStatus;
    private String orderDate;


    // Getters and Setters
    public Integer getStoreOrderId() {
        return storeOrderId;
    }

    public void setStoreOrderId(Integer storeOrderId) {
        this.storeOrderId = storeOrderId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVendorCouponCode() {
        return vendorCouponCode;
    }

    public void setVendorCouponCode(String vendorCouponCode) {
        this.vendorCouponCode = vendorCouponCode;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(Integer totalDiscount) {
        this.totalDiscount = totalDiscount;
    }

    public Integer getTotalNetAmount() {
        return totalNetAmount;
    }

    public void setTotalNetAmount(Integer totalNetAmount) {
        this.totalNetAmount = totalNetAmount;
    }

    public List<VendorUserOrderDTO> getOrders() {
        return orders;
    }

    public void setOrders(List<VendorUserOrderDTO> orders) {
        this.orders = orders;
    }

    public String getStoreOrderStatus() {
        return storeOrderStatus;
    }

    public void setStoreOrderStatus(String storeOrderStatus) {
        this.storeOrderStatus = storeOrderStatus;
    }

    public String getOrderPayStatus() {
        return orderPayStatus;
    }

    public void setOrderPayStatus(String orderPayStatus) {
        this.orderPayStatus = orderPayStatus;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }
}
