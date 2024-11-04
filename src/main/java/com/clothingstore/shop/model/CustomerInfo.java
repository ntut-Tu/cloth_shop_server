package com.clothingstore.shop.model;


public class CustomerInfo {

    private int customerId;

    private User user;

    private String defaultShippingAddress;
    private String billingAddress;
    private boolean isActive;

    public CustomerInfo() {}

    public CustomerInfo(User user, String defaultShippingAddress, String billingAddress, boolean isActive) {
        this.user = user;
        this.defaultShippingAddress = defaultShippingAddress;
        this.billingAddress = billingAddress;
        this.isActive = isActive;
    }

    public int getCustomerId() {
        return customerId;
    }

    public User getUser() {
        return user;
    }

    public String getDefaultShippingAddress() {
        return defaultShippingAddress;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setDefaultShippingAddress(String defaultShippingAddress) {
        this.defaultShippingAddress = defaultShippingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }
}
