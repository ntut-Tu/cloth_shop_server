package com.clothingstore.shop.dto.others.tempOrder;

import com.clothingstore.shop.dto.others.discount.DiscountDetailsDTO;
import com.clothingstore.shop.dto.request.checkout.SubmitOrderRequestDTO;
import com.clothingstore.shop.exceptions.SharedException;
import com.clothingstore.shop.repository.DiscountRepository;

import java.util.List;

public class TemporaryOrder {
    private final DiscountRepository discountRepository;

    private Integer customerId;
    private List<TemporaryStoreOrder> storeOrders;
    private Integer totalAmount;
    private Integer subtotal;
    private Integer shippingFee;
    private Integer shippingDiscountAmount;
    private String shippingDiscountCode;
    private Integer totalStoreDiscountAmount;
    private long expirationTime; // 用於超時清理

    public TemporaryOrder(DiscountRepository discountRepository) {
        this.discountRepository = discountRepository;
    }


    // Getters and Setters
    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public List<TemporaryStoreOrder> getStoreOrders() {
        return storeOrders;
    }

    public void setStoreOrders(List<TemporaryStoreOrder> storeOrders) {
        this.storeOrders = storeOrders;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Integer subtotal) {
        this.subtotal = subtotal;
    }

    public Integer getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(Integer shippingFee) {
        this.shippingFee = shippingFee;
    }

    public Integer getShippingDiscountAmount() {
        return shippingDiscountAmount;
    }

    public void setShippingDiscountAmount(Integer shippingDiscountAmount) {
        this.shippingDiscountAmount = shippingDiscountAmount;
    }

    public Integer getTotalStoreDiscountAmount() {
        return totalStoreDiscountAmount;
    }

    public void setTotalStoreDiscountAmount(Integer totalStoreDiscountAmount) {
        this.totalStoreDiscountAmount = totalStoreDiscountAmount;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public void addStoreDiscount(int storeId, DiscountDetailsDTO storeDiscount) {
        for (TemporaryStoreOrder storeOrder : storeOrders) {
            if (storeOrder.getStoreId().equals(storeId)) {
                storeOrder.setDiscountDetails(storeDiscount);
                break;
            }
        }
    }

    public Integer getShippingDiscountId() throws SharedException {
        return discountRepository.queryDiscountIdByCode(shippingDiscountCode);
    }

    public void setShippingDiscountCode(String shippingDiscountCode) {
        this.shippingDiscountCode = shippingDiscountCode;
    }

}

