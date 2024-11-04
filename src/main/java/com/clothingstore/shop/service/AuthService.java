package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.RegisterRequest;
import com.clothingstore.shop.model.User;
import com.clothingstore.shop.repository.AuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final AuthRepository authRepository;

    @Autowired
    public AuthService(JwtService jwtService, AuthRepository authRepository) {
        this.jwtService = jwtService;
        this.authRepository = authRepository;
    }

    public String authenticate(String account, String password, String role) {
        var userOpt = authRepository.findByUserAccount(account,role);

        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();

        if (user.getPassword().equals(password) &&
                user.getUserType().equals(role) &&
                user.getIsActive()) {
            return jwtService.generateJwtToken(account, role); // 生成 JWT token
        }

        return null;
    }

    public boolean checkUserExists(String username, String role) {
        return authRepository.findByUserAccountAndRole(username, role).isPresent();
    }

    public boolean register(RegisterRequest registerRequest) {
        if (authRepository.findByUserAccountAndRole(registerRequest.getAccount(), registerRequest.getRole()).isPresent()) {
            return false;
        }

        User newUser = new User();
        newUser.setAccount(registerRequest.getAccount());
        newUser.setPassword(registerRequest.getPassword());
        newUser.setEmail(registerRequest.getEmail());
        newUser.setUserType(registerRequest.getRole());
        newUser.setCreatedAt(OffsetDateTime.now());
        newUser.setIsActive(true);

        return authRepository.saveUser(newUser, registerRequest);
    }
}
