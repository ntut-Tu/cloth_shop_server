package com.clothingstore.shop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {
    private final JwtService jwtService;
    @Autowired
    public CheckoutService(JwtService jwtService) {
        this.jwtService = jwtService;
    }
}
