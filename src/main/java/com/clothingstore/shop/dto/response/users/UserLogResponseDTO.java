package com.clothingstore.shop.dto.response.users;

public class UserLogResponseDTO {
    Integer log_id;
    Integer user_id;
    String username;
    String action;
    String date;

    //Getters and Setters
    // Getters and Setters
    public Integer getLogId() {
        return log_id;
    }

    public void setLogId(Integer log_id) {
        this.log_id = log_id;
    }

    public Integer getUserId() {
        return user_id;
    }

    public void setUserId(Integer user_id) {
        this.user_id = user_id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
