package com.clothingstore.shop.model;


public class CustomerInfo {

    private Integer customerId;

    private User user;

    private String defaultShippingAddress;
    private String billingAddress;

    public CustomerInfo() {}

    public CustomerInfo(User user, String defaultShippingAddress, String billingAddress) {
        this.user = user;
        this.defaultShippingAddress = defaultShippingAddress;
        this.billingAddress = billingAddress;
    }

    public Integer getCustomerId() {
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

    public void setCustomerId(Integer customerId) {
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
}
