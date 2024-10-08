package com.clothingstore.shop.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class AuthService {

    private final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public String authenticate(String username, String password, String role) {
        if ("admin".equals(username) && "123".equals(password) && "admin".equals(role)) {
            return generateJwtToken(username, role);
        }
        if ("customer".equals(username) && "123".equals(password) && "customer".equals(role)) {
            return generateJwtToken(username, role);
        }
        if ("vendor".equals(username) && "123".equals(password) && "vendor".equals(role)) {
            return generateJwtToken(username, role);
        }
        return null;
    }

    public boolean register(String username, String email, String password, String role) {
        return true;
    }

    private String generateJwtToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(SECRET_KEY)  // 使用安全的密钥进行签名
                .compact();
    }
}
