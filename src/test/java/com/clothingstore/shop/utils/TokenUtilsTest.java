package com.clothingstore.shop.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenUtilsTest {

    @Test
    void cookieTakesPrecedenceOverAuthorizationHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "cookie-token")});
        when(request.getHeader("Authorization")).thenReturn("Bearer header-token");

        assertEquals("cookie-token", TokenUtils.extractToken(request));
    }

    @Test
    void acceptsBearerHeaderWhenCookieIsMissingOrBlank() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "")});
        when(request.getHeader("Authorization")).thenReturn("  bEaReR   header-token  ");

        assertEquals("header-token", TokenUtils.extractToken(request));
    }

    @Test
    void rejectsMalformedAuthorizationHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token extra");

        assertNull(TokenUtils.extractToken(request));
    }

    @Test
    void returnsNullWhenNoCredentialsAreSupplied() {
        assertNull(TokenUtils.extractToken(mock(HttpServletRequest.class)));
    }

    @Test
    void legacyCookieExtractorAlsoAcceptsBearerHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer header-token");

        assertEquals("header-token", TokenUtils.extractTokenFromCookies(request));
    }
}
