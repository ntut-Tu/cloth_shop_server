package com.clothingstore.shop.dto.repository.users;


public class CustomerInfoRepositoryDTO {

    private Integer customerId;

    private UserRepositoryDTO userRepositoryDTO;

    private String defaultShippingAddress;
    private String billingAddress;

    public CustomerInfoRepositoryDTO() {}

    public CustomerInfoRepositoryDTO(UserRepositoryDTO userRepositoryDTO, String defaultShippingAddress, String billingAddress) {
        this.userRepositoryDTO = userRepositoryDTO;
        this.defaultShippingAddress = defaultShippingAddress;
        this.billingAddress = billingAddress;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public UserRepositoryDTO getUser() {
        return userRepositoryDTO;
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

    public void setUser(UserRepositoryDTO userRepositoryDTO) {
        this.userRepositoryDTO = userRepositoryDTO;
    }

    public void setDefaultShippingAddress(String defaultShippingAddress) {
        this.defaultShippingAddress = defaultShippingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }
}
