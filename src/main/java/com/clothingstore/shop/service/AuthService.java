package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.request.auth.RegisterRequestDTO;
import com.clothingstore.shop.dto.repository.users.UserRepositoryDTO;
import com.clothingstore.shop.repository.AuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

        UserRepositoryDTO userRepositoryDTO = userOpt.get();

        if (userRepositoryDTO.getPassword().equals(password) &&
                userRepositoryDTO.getUserType().equals(role) &&
                userRepositoryDTO.getIsActive()) {
            return jwtService.generateJwtToken(account, role, userRepositoryDTO.getId()); // 生成 JWT token
        }

        return null;
    }

    public boolean checkUserExists(String username, String role) {
        return authRepository.findByUserAccountAndRole(username, role).isPresent();
    }

    public boolean checkUserExists(Integer userId,String role) {
        return authRepository.findByUserIDAndRole(userId, role)
                .map(user -> Boolean.TRUE.equals(user.getIsActive())).orElse(false);
    }

    @Transactional
    public boolean register(RegisterRequestDTO registerRequestDTO) {
        // Administrative identities are provisioned outside the public registration flow.
        if (registerRequestDTO == null ||
                !("customer".equals(registerRequestDTO.getRole()) || "vendor".equals(registerRequestDTO.getRole()))) {
            return false;
        }
        if (authRepository.findByUserAccountAndRole(registerRequestDTO.getAccount(), registerRequestDTO.getRole()).isPresent()) {
            return false;
        }

        UserRepositoryDTO newUserRepositoryDTO = new UserRepositoryDTO();
        newUserRepositoryDTO.setAccount(registerRequestDTO.getAccount());
        newUserRepositoryDTO.setPassword(registerRequestDTO.getPassword());
        newUserRepositoryDTO.setEmail(registerRequestDTO.getEmail());
        newUserRepositoryDTO.setUserType(registerRequestDTO.getRole());
        newUserRepositoryDTO.setCreatedAt(OffsetDateTime.now());
        newUserRepositoryDTO.setIsActive(true);

        return authRepository.saveUser(newUserRepositoryDTO, registerRequestDTO);
    }

    public  Integer getCustomerId(Integer userId) {
        return authRepository.getCustomerId(userId);
    }

    public Integer getVendorId(Integer userId) {
        return authRepository.getVendorId(userId);
    }

    public Integer getAdminId(Integer userId) {
        return authRepository.getAdminId(userId);
    }
}
