package com.clothingstore.shop.dto.repository.users;


public class VendorInfoRepositoryDTO {

    private Integer vendorId;


    private UserRepositoryDTO userRepositoryDTO;

    private String storeAddress;
    private String storeDescription;
    private String storeLogoUrl;
    private String paymentAccount;

    public VendorInfoRepositoryDTO() {}

    public VendorInfoRepositoryDTO(UserRepositoryDTO userRepositoryDTO, String storeAddress, String storeDescription, String storeLogoUrl, String paymentAccount) {
        this.userRepositoryDTO = userRepositoryDTO;
        this.storeAddress = storeAddress;
        this.storeDescription = storeDescription;
        this.storeLogoUrl = storeLogoUrl;
        this.paymentAccount = paymentAccount;
    }

    public Integer getVendorId() {
        return vendorId;
    }

    public UserRepositoryDTO getUser() {
        return userRepositoryDTO;
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

    public void setVendorId(Integer vendorId) {
        this.vendorId = vendorId;
    }

    public void setUser(UserRepositoryDTO userRepositoryDTO) {
        this.userRepositoryDTO = userRepositoryDTO;
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

}
