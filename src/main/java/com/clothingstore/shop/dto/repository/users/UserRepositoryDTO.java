package com.clothingstore.shop.dto.repository.users;


import java.time.OffsetDateTime;

public class UserRepositoryDTO {
    private Integer id;
    private String account;
    private String password;
    private String email;
    private boolean isActive;
    private String userType;
    private OffsetDateTime createdAt;
    private String phoneNumber;

    private VendorInfoRepositoryDTO vendorInfoRepositoryDTO;
    private CustomerInfoRepositoryDTO customerInfoRepositoryDTO;
    private AdminInfoRepositoryDTO adminInfoRepositoryDTO;

    public UserRepositoryDTO() {}

    public UserRepositoryDTO(Integer id, String account, String password, String email, boolean isActive, String userType,
                             OffsetDateTime createdAt, String phoneNumber) {
        this.id = id;
        this.account = account;
        this.password = password;
        this.email = email;
        this.isActive = isActive;
        this.userType = userType;
        this.createdAt = createdAt;
        this.phoneNumber = phoneNumber;
    }

    public Integer getId() {
        return id;
    }

    public String getAccount() {
        return account;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public String getUserType() {
        return userType;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public VendorInfoRepositoryDTO getVendorInfo() {
        return vendorInfoRepositoryDTO;
    }

    public CustomerInfoRepositoryDTO getCustomerInfo() {
        return customerInfoRepositoryDTO;
    }

    public AdminInfoRepositoryDTO getAdminInfo() {
        return adminInfoRepositoryDTO;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setVendorInfo(VendorInfoRepositoryDTO vendorInfoRepositoryDTO) {
        this.vendorInfoRepositoryDTO = vendorInfoRepositoryDTO;
    }

    public void setCustomerInfo(CustomerInfoRepositoryDTO customerInfoRepositoryDTO) {
        this.customerInfoRepositoryDTO = customerInfoRepositoryDTO;
    }

    public void setAdminInfo(AdminInfoRepositoryDTO adminInfoRepositoryDTO) {
        this.adminInfoRepositoryDTO = adminInfoRepositoryDTO;
    }
}