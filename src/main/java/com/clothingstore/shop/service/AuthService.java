package com.clothingstore.shop.service;

import com.clothingstore.shop.repository.AuthRepository;
import com.clothingstore.shop.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final AuthRepository authRepository;

    @Autowired
    public AuthService(JwtService jwtService, AuthRepository authRepository) {
        this.jwtService = jwtService;
        this.authRepository = authRepository;
    }

    public String authenticate(String username, String password, String role) {
        var userOpt = authRepository.findByUserAccount(username);

        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();

        if (user.getPassword().equals(password) &&
                user.getUserType().equals(role) &&
                user.getIsActive()) {
            return jwtService.generateJwtToken(username, role); // 生成 JWT token
        }

        return null;
    }

    public boolean register(String username, String email, String password, String role) {
        return true;
    }
}
