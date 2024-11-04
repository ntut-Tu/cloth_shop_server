package com.clothingstore.shop.model;


public class VendorInfo {

    private int vendorId;


    private User user;

    private String storeAddress;
    private String storeDescription;
    private String storeLogoUrl;
    private String paymentAccount;
    private boolean isActive;

    public VendorInfo() {}

    public VendorInfo(User user, String storeAddress, String storeDescription, String storeLogoUrl, String paymentAccount, boolean isActive) {
        this.user = user;
        this.storeAddress = storeAddress;
        this.storeDescription = storeDescription;
        this.storeLogoUrl = storeLogoUrl;
        this.paymentAccount = paymentAccount;
        this.isActive = isActive;
    }

    public int getVendorId() {
        return vendorId;
    }

    public User getUser() {
        return user;
    }

    public String getStoreAddress() {
        return storeAddress;
    }

    public String getStoreDescription() {
        return storeDescription;
    }

    public String getStoreLogoUrl() {
        return storeLogoUrl;
    }

    public String getPaymentAccount() {
        return paymentAccount;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setVendorId(int vendorId) {
        this.vendorId = vendorId;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setStoreAddress(String storeAddress) {
        this.storeAddress = storeAddress;
    }

    public void setStoreDescription(String storeDescription) {
        this.storeDescription = storeDescription;
    }

    public void setStoreLogoUrl(String storeLogoUrl) {
        this.storeLogoUrl = storeLogoUrl;
    }

    public void setPaymentAccount(String paymentAccount) {
        this.paymentAccount = paymentAccount;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }
}
