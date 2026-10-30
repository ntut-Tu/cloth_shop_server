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
    public List<UserSummaryResponseDTO> getUsers(Integer page, Integer size, String search, String token) {
        Integer userId = jwtService.extractUserId(token);
        if (!authService.checkUserExists(userId,"admin")) {
            throw new IllegalArgumentException("User is not authorized to view users.");
        }
        if (page < 0 || size < 1 || size > 100 || page > Integer.MAX_VALUE / size) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        String normalizedSearch = search == null ? null : search.trim();
        if (normalizedSearch != null && normalizedSearch.length() > 100) {
            throw new IllegalArgumentException("Search must be at most 100 characters.");
        }
        return userRepository.fetchUsers(page, size, normalizedSearch);
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
