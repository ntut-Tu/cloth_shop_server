package com.clothingstore.shop.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public class TokenUtils {

    @Deprecated
    public static String extractTokenFromCookies(HttpServletRequest request) {
        return extractToken(request);
    }

    public static String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }

        String authorization = request.getHeader("Authorization");
        if (authorization != null) {
            String[] parts = authorization.trim().split("\\s+");
            if (parts.length == 2 && "Bearer".equalsIgnoreCase(parts[0])) {
                return parts[1];
            }
        }
        return null;
    }
}
