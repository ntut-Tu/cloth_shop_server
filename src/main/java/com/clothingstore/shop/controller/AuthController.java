package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.ApiResponse;
import com.clothingstore.shop.dto.JwtResponse;
import com.clothingstore.shop.dto.LoginRequest;
import com.clothingstore.shop.dto.RegisterRequest;
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
    public ResponseEntity<ApiResponse<JwtResponse>> login(@RequestBody LoginRequest loginRequest) {
        String token = authService.authenticate(loginRequest.getUsername(), loginRequest.getPassword(), loginRequest.getRole());
        if (token != null) {
            return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", new JwtResponse(token)));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Invalid credentials", null));
        }
    }

    @PostMapping("/checkUser")
    public ResponseEntity<ApiResponse<Object>> checkUser(@RequestBody RegisterRequest registerRequest) {
        boolean exists = authService.checkUserExists(registerRequest.getAccount(), registerRequest.getRole());
        if (exists) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(true, "User already exists", null));
        } else {
            return ResponseEntity.ok(new ApiResponse<>(false, "User does not exist", null));
        }
    }

    @PostMapping("/registerDetails")
    public ResponseEntity<ApiResponse<Object>> registerDetails(@RequestBody RegisterRequest registerRequest) {
        boolean success = authService.register(registerRequest);
        if (success) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "User registered successfully", null));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, "Registration failed. User may already exist.", null));
        }
    }
}
