package com.clothingstore.shop.dto.response.auth;

public record CurrentUserResponseDTO(Integer userId, String account, String email, String role) {
}
