package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendor/products")
public class VendorProductController {

    private final ProductService productService;

    @Autowired
    public VendorProductController(ProductService productService) {
        this.productService = productService;
    }



}
