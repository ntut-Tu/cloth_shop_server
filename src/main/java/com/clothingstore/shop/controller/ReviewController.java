package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.request.review.AddReviewRequestDTO;
import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.response.review.AddReviewResponseDTO;
import com.clothingstore.shop.service.ReviewService;
import com.clothingstore.shop.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
public class ReviewController {
    private final ReviewService reviewService;
    @Autowired
    ReviewController(ReviewService reviewService){
        this.reviewService = reviewService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDTO<AddReviewResponseDTO>> addReview(
            HttpServletRequest request,
            @RequestBody AddReviewRequestDTO addReviewRequestDTO){
        try {
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            AddReviewResponseDTO ret = reviewService.addReview(addReviewRequestDTO, token);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Review added successfully", ret));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }
}
