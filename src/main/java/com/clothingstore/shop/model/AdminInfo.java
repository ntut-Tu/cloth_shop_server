package com.clothingstore.shop.model;


public class AdminInfo {

    private int adminId;

    private User user;

    public AdminInfo() {}

    public AdminInfo(User user) {
        this.user = user;
    }

    public int getAdminId() {
        return adminId;
    }

    public User getUser() {
        return user;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
