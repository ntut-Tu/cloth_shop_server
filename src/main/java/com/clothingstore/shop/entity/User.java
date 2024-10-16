package com.clothingstore.shop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String account;
    private String password;
    private String email;
    private boolean isDisable;
    private String userType;

    public User() {}

    public User(int id, String account, String password, String email, boolean isDisable, String userType) {
        this.id = id;
        this.account = account;
        this.password = password;
        this.email = email;
        this.isDisable = isDisable;
        this.userType = userType;
    }


    public int getId() {
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

    public boolean getIsDisable() {
        return isDisable;
    }

    public String getUserType() {
        return userType;
    }

    public void setId(int id) {
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

    public void setIsDisable(boolean isDisable) {
        this.isDisable = isDisable;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}
