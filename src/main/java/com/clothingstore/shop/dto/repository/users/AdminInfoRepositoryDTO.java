package com.clothingstore.shop.dto.repository.users;


public class AdminInfoRepositoryDTO {

    private Integer adminId;

    private UserRepositoryDTO userRepositoryDTO;

    public AdminInfoRepositoryDTO() {}

    public AdminInfoRepositoryDTO(UserRepositoryDTO userRepositoryDTO) {
        this.userRepositoryDTO = userRepositoryDTO;
    }

    public Integer getAdminId() {
        return adminId;
    }

    public UserRepositoryDTO getUser() {
        return userRepositoryDTO;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    public void setUser(UserRepositoryDTO userRepositoryDTO) {
        this.userRepositoryDTO = userRepositoryDTO;
    }
}
