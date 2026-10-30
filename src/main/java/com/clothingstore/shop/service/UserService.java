package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.response.users.UserSummaryResponseDTO;
import com.clothingstore.shop.dto.response.users.UserLogResponseDTO;
import com.clothingstore.shop.repository.UserRepository;
import com.clothingstore.shop.security.Privilege;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
@Service
public class UserService {
    private final RbacService rbacService;
    private final UserRepository userRepository;
    @Autowired
    public UserService(RbacService rbacService, UserRepository userRepository) {
        this.rbacService = rbacService;
        this.userRepository = userRepository;
    }
    public List<UserSummaryResponseDTO> getUsers(Integer page,Integer size,String token) {
        rbacService.requirePermission(token, Privilege.ADMIN_USERS_READ);
        return userRepository.fetchUsers(page,size);
    }

    public Boolean banUser(Integer banUserId, String token) {
        int userId = rbacService.requirePermission(token, Privilege.ADMIN_USERS_BAN);
        if (userId == banUserId) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot ban your own account");
        }
        return userRepository.banUser(banUserId);
    }

    public List<UserLogResponseDTO> getUserLogs(int page, int size, String token) {
        rbacService.requirePermission(token, Privilege.ADMIN_LOGS_READ);
        return userRepository.fetchUserLogs(page, size);
    }
}
