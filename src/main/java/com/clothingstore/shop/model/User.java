package com.clothingstore.shop.model;

import java.time.OffsetDateTime;

public class User {
    private int id;
    private String account;
    private String password;
    private String email;
    private OffsetDateTime createdAt;
    private String phoneNumber;
    private String userType;

    public User() {}

    public User(int id, String account, String password, String email, OffsetDateTime createdAt, String phoneNumber, String userType) {
        this.id = id;
        this.account = account;
        this.password = password;
        this.email = email;
        this.createdAt = createdAt;
        this.phoneNumber = phoneNumber;
        this.userType = userType;
    }

    // Getters and setters for each field
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}
