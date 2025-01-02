package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.request.review.AddReviewRequestDTO;
import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.dto.response.review.AddReviewResponseDTO;
import com.clothingstore.shop.dto.response.review.GetReviewResponseDTO;
import com.clothingstore.shop.service.ReviewService;
import com.clothingstore.shop.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/getProductReviews/{page}")
    public ResponseEntity<ApiResponseDTO<List<GetReviewResponseDTO>>> getProductReviews(
            @RequestParam Integer productId,
            @PathVariable Integer page
    ){
        try {
            List<GetReviewResponseDTO> ret = reviewService.getProductReviews(productId,page);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Reviews fetched successfully", ret));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/isReviewExist")
    public ResponseEntity<ApiResponseDTO<Boolean>> isReviewExist(
            @RequestParam Integer productId,
            HttpServletRequest request
    ){
        try {
            String token = TokenUtils.extractTokenFromCookies(request);
            if (token == null) {
                throw new IllegalArgumentException("Token not found");
            }
            Boolean ret = reviewService.isReviewExist(productId, token);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Review exist", ret));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponseDTO<>(false, e.getMessage(), null));
        }
    }
}
