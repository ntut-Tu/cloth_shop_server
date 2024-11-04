package com.clothingstore.shop.model;


public class AdminInfo {

    private Integer adminId;

    private User user;

    public AdminInfo() {}

    public AdminInfo(User user) {
        this.user = user;
    }

    public Integer getAdminId() {
        return adminId;
    }

    public User getUser() {
        return user;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
