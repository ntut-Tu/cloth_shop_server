package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.request.review.AddReviewRequestDTO;
import com.clothingstore.shop.dto.response.review.AddReviewResponseDTO;
import com.clothingstore.shop.dto.response.review.GetReviewResponseDTO;
import com.clothingstore.shop.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {
    private final JwtService jwtService;
    private final ReviewRepository reviewRepository;
    private final AuthService authService;

    @Autowired
    ReviewService(JwtService jwtService, ReviewRepository reviewRepository, AuthService authService) {
        this.jwtService = jwtService;
        this.reviewRepository = reviewRepository;
        this.authService = authService;
    }
    public AddReviewResponseDTO addReview(AddReviewRequestDTO addReviewRequestDTO, String token) throws Exception {
        try {
            Integer userId = jwtService.extractUserId(token);
            AddReviewResponseDTO ret = reviewRepository.addReview(userId, addReviewRequestDTO.getProductId(), addReviewRequestDTO.getComment(), BigDecimal.valueOf(addReviewRequestDTO.getRating()));
            return ret;
        }catch (Exception e){
            throw e;
        }
    }

    public List<GetReviewResponseDTO> getProductReviews(Integer productId,Integer page) {
        try {
            return reviewRepository.getProductReviews(productId,page);
        }catch (Exception e){
            throw e;
        }
    }

    public Boolean isReviewExist(Integer productId, String token) {
        try {
            Integer userId = jwtService.extractUserId(token);
            return reviewRepository.isReviewExist(authService.getCustomerId(userId), productId);
        }catch (Exception e){
            throw e;
        }
    }
}
