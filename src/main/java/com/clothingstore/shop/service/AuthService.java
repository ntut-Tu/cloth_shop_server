package com.clothingstore.shop.service;

import com.clothingstore.shop.repository.AuthRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class AuthService {

    @Autowired
    private JwtService jwtService;
//    @Autowired
//    private AuthRepository authRepository;

    public String authenticate(String username, String password, String role) {
//        var user = authRepository.findByUserAccount(username);
//        if(user.isPresent()){
//            if(user.get().getPassword().equals(password) && user.get().getUserType().equals(role) && !user.get().getIsDisable()){
//                return jwtService.generateJwtToken(username,role);
//            }else {
//                return null;
//            }
//        }else {
//            return null;
//        }
        if ("admin".equals(username) && "123".equals(password) && "admin".equals(role)) {
            return jwtService.generateJwtToken(username, role);
        }
        if ("customer".equals(username) && "123".equals(password) && "customer".equals(role)) {
            return jwtService.generateJwtToken(username, role);
        }
        if ("vendor".equals(username) && "123".equals(password) && "vendor".equals(role)) {
            return jwtService.generateJwtToken(username, role);
        }
        return null;
    }

    public boolean register(String username, String email, String password, String role) {
        return true;
    }

}
