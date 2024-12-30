package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.request.userData.EditUserDataRequestDTO;
import com.clothingstore.shop.dto.response.userData.EditUserDataResponseDTO;
import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.service.EditUserDataService;
import com.clothingstore.shop.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EditUserDataController {
    private final EditUserDataService editUserDataService;
    @Autowired
    EditUserDataController(EditUserDataService editUserDataService){
        this.editUserDataService = editUserDataService;
    }

    @PostMapping("/editUserData")
    public ResponseEntity<ApiResponseDTO<EditUserDataResponseDTO>> editUserData(
            HttpServletRequest request,
            @RequestBody EditUserDataRequestDTO editUserDataRequestDTO){
        try {
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            EditUserDataResponseDTO ret = editUserDataService.editUserData(editUserDataRequestDTO, token);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "User data edited successfully", ret));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

}
