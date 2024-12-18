package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.response.users.UserSummaryResponseDTO;
import com.clothingstore.shop.dto.response.users.UserLogResponseDTO;
import com.clothingstore.shop.service.UserService;
import com.clothingstore.shop.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserService userService;
    @Autowired
    public AdminController(UserService userService) {
        this.userService = userService;
    }
    @GetMapping("/users")
    public ResponseEntity<ApiResponseDTO<List<UserSummaryResponseDTO>>> getUsers(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            List<UserSummaryResponseDTO> users = userService.getUsers(page,size,token);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Users fetched successfully", users));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/banUser/{userId}")
    public ResponseEntity<ApiResponseDTO<Object>> banUser(
            HttpServletRequest request,
            @PathVariable Integer userId
    ) {
        try {
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            Boolean banned = userService.banUser(userId, token);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "User banned successfully", banned));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }


    @GetMapping("/userLogs")
    public ResponseEntity<ApiResponseDTO<List<UserLogResponseDTO>>> getUserLogs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            List<UserLogResponseDTO> userLogs = userService.getUserLogs(page,size,token);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "User logs fetched successfully", userLogs));
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }
}
