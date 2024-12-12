package com.clothingstore.shop.service;

import com.clothingstore.shop.dto.request.review.AddReviewRequestDTO;
import com.clothingstore.shop.dto.response.review.AddReviewResponseDTO;
import com.clothingstore.shop.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class ReviewService {
    private final JwtService jwtService;
    private final ReviewRepository reviewRepository;

    @Autowired
    ReviewService(JwtService jwtService, ReviewRepository reviewRepository){
        this.jwtService = jwtService;
        this.reviewRepository = reviewRepository;
    }
    public AddReviewResponseDTO addReview(AddReviewRequestDTO addReviewRequestDTO, String token) throws Exception{
        try{
            Integer userId = jwtService.extractUserId(token);
            Map<Integer, OffsetDateTime> retMap=reviewRepository.addReview(userId,addReviewRequestDTO.getProductId(),addReviewRequestDTO.getComment(), BigDecimal.valueOf(addReviewRequestDTO.getRating()) );
            AddReviewResponseDTO ret = new AddReviewResponseDTO();
            Object[] retArray = retMap.keySet().toArray();
            ret.setReviewId((Integer) retArray[0]);
            ret.setReviewDate(retArray[1].toString());
            return ret;
        }catch (Exception e){
            throw e;
        }
    }
}
