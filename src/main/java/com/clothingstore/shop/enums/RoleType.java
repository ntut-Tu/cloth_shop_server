package com.clothingstore.shop.enums;

public enum RoleType {
    CUSTOMER("customer"),
    ADMIN("admin"),
    VENDOR("vendor");

    private final String role;

    RoleType(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}
