package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.request.userData.EditUserDataRequestDTO;
import com.clothingstore.shop.dto.response.userData.EditUserDataResponseDTO;
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
    public EditUserDataResponseDTO editUserData(EditUserDataRequestDTO editUserDataRequestDTO, String token) throws Exception {
        try {
            Integer userId = jwtService.extractUserId(token);
            EditUserDataResponseDTO ret = editUserDataRepository.editUserData(editUserDataRequestDTO.getAccount(), editUserDataRequestDTO.getPassword(), editUserDataRequestDTO.getEmail(), editUserDataRequestDTO.getPhoneNumber());
            return ret;
        }catch (Exception e){
            throw e;
        }
    }
}
