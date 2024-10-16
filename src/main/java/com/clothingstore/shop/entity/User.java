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
    private boolean is_disabled;
    private String user_type;

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

    public boolean getIs_disabled() {
        return is_disabled;
    }

    public String getUser_type() {
        return user_type;
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

    public void setIs_disabled(boolean is_disabled) {
        this.is_disabled = is_disabled;
    }

    public void setUser_type(String user_type) {
        this.user_type = user_type;
    }
}
