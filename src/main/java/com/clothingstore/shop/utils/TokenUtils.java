package com.clothingstore.shop.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public class TokenUtils {

    /**
     * 從 cookie 中提取 token
     * @param request - HttpServletRequest 請求對象
     * @return token 字串，如果 cookie 中存在則返回，否則返回 null
     */
    public static String extractTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
