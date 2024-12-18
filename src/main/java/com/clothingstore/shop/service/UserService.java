package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.response.users.UserSummaryResponseDTO;
import com.clothingstore.shop.dto.response.users.UserLogResponseDTO;
import com.clothingstore.shop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class UserService {
    private final JwtService jwtService;
    private final AuthService authService;
    private final UserRepository userRepository;
    @Autowired
    public UserService(JwtService jwtService, AuthService authService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.authService = authService;
        this.userRepository = userRepository;
    }
    public List<UserSummaryResponseDTO> getUsers(Integer page,Integer size,String token) {
        Integer userId = jwtService.extractUserId(token);
        if (!authService.checkUserExists(userId,"admin")) {
            throw new IllegalArgumentException("User is not authorized to view users.");
        }
        return userRepository.fetchUsers(page,size);
    }

    public Boolean banUser(Integer banUserId, String token) {
        Integer userId = jwtService.extractUserId(token);
        if (!authService.checkUserExists(userId,"admin")) {
            throw new IllegalArgumentException("User is not authorized to ban users.");
        }
        return userRepository.banUser(banUserId);
    }

    public List<UserLogResponseDTO> getUserLogs(int page, int size, String token) {
        Integer userId = jwtService.extractUserId(token);
        if (!authService.checkUserExists(userId, "admin")) {
            throw new IllegalArgumentException("User is not authorized to view logs.");
        }
        return userRepository.fetchUserLogs(page, size);
    }
}
