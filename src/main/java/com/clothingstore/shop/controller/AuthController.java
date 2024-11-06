package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.response.JwtResponseDTO;
import com.clothingstore.shop.dto.request.LoginRequestDTO;
import com.clothingstore.shop.dto.request.RegisterRequestDTO;
import com.clothingstore.shop.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<JwtResponseDTO>> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        String token = authService.authenticate(loginRequestDTO.getUsername(), loginRequestDTO.getPassword(), loginRequestDTO.getRole());
        if (token != null) {
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Login successful", new JwtResponseDTO(token)));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDTO<>(false, "Invalid credentials", null));
        }
    }

    @PostMapping("/checkUser")
    public ResponseEntity<ApiResponseDTO<Object>> checkUser(@RequestBody RegisterRequestDTO registerRequestDTO) {
        boolean exists = authService.checkUserExists(registerRequestDTO.getAccount(), registerRequestDTO.getRole());
        if (exists) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(true, "User already exists", null));
        } else {
            return ResponseEntity.ok(new ApiResponseDTO<>(false, "User does not exist", null));
        }
    }

    @PostMapping("/registerDetails")
    public ResponseEntity<ApiResponseDTO<Object>> registerDetails(@RequestBody RegisterRequestDTO registerRequestDTO) {
        boolean success = authService.register(registerRequestDTO);
        if (success) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDTO<>(true, "User registered successfully", null));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseDTO<>(false, "Registration failed. User may already exist.", null));
        }
    }
}
