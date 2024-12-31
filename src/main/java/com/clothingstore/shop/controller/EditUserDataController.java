package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.request.userDataOperation.EditUserDataRequestDTO;
import com.clothingstore.shop.dto.response.userDataOperation.EditUserDataResponseDTO;
import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.service.EditUserDataService;
import com.clothingstore.shop.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/editUserData")
public class EditUserDataController {
    private final EditUserDataService editUserDataService;
    @Autowired
    EditUserDataController(EditUserDataService editUserDataService){
        this.editUserDataService = editUserDataService;
    }

    @PostMapping("/")
    public ResponseEntity<ApiResponseDTO<EditUserDataResponseDTO>> editUserData(
            HttpServletRequest request,
            @RequestBody EditUserDataRequestDTO editUserDataRequestDTO){
        try {
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            EditUserDataResponseDTO ret = editUserDataService.editUserData(editUserDataRequestDTO, token);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "edit user data successfully", ret));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/")
    public ResponseEntity<ApiResponseDTO<EditUserDataResponseDTO>> getUserData(HttpServletRequest request){
        try {
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            EditUserDataResponseDTO ret = editUserDataService.getUserData(token);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Get user data successfully", ret));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }
}
