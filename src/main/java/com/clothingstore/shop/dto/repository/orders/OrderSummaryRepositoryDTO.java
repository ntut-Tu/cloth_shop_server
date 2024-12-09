package com.clothingstore.shop.dto.repository.orders;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class OrderSummaryRepositoryDTO {
    private Integer orderId;
    private LocalDateTime orderDate;
    private Integer totalAmount;
    private String payStatus;
    private String shipStatus;
    private String shippingDiscountCode;
    OrderSummeryDetailModel orderSummeryDetailModel;
    // Getters and Setters
    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public LocalDateTime  getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime  orderDate) {
        this.orderDate = orderDate;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
    }

    public String getShipStatus() {
        return shipStatus;
    }

    public void setShipStatus(String shipStatus) {
        this.shipStatus = shipStatus;
    }

    public String getShippingDiscountCode() {
        return shippingDiscountCode;
    }

    public void setShippingDiscountCode(String shippingDiscountCode) {
        this.shippingDiscountCode = shippingDiscountCode;
    }

    public OrderSummeryDetailModel getOrderSummeryDetailModel() {
        return orderSummeryDetailModel;
    }

    public void setOrderSummeryDetailModel(OrderSummeryDetailModel orderSummeryDetailModel) {
        this.orderSummeryDetailModel = orderSummeryDetailModel;
    }

}
