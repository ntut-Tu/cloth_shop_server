package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.request.editUserData.EditUserDataRequestDTO;
import com.clothingstore.shop.dto.response.editUserData.EditUserDataResponseDTO;
import com.clothingstore.shop.repository.EditUserDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EditUserDataService {
    private final JwtService jwtService;
    private final EditUserDataRepository editUserDataRepository;

    @Autowired
    EditUserDataService(JwtService jwtService, EditUserDataRepository editUserDataRepository){
        this.jwtService = jwtService;
        this.editUserDataRepository = editUserDataRepository;
    }
    // edit user data by user_id
    public EditUserDataResponseDTO editUserData(EditUserDataRequestDTO editUserDataRequestDTO, String token) throws Exception {
        try {
            Integer userId = jwtService.extractUserId(token);
            return editUserDataRepository.editUserData(userId, editUserDataRequestDTO.getAccount(), editUserDataRequestDTO.getPassword(), editUserDataRequestDTO.getEmail(), editUserDataRequestDTO.getPhoneNumber(), editUserDataRequestDTO.getProfilePicUrl());
        }catch (Exception e){
            throw e;
        }
    }
    // get user data by user_id
    public EditUserDataResponseDTO getUserData(String token) throws Exception {
        try {
            Integer userId = jwtService.extractUserId(token);
            return editUserDataRepository.getUserData(userId);
        }catch (Exception e){
            throw e;
        }
    }
}
