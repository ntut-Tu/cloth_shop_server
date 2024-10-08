package com.clothingstore.shop.dto;

public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String role;

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getPassword() {
        return password;
    }
}
