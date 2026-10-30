package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.promotion.*;
import com.clothingstore.shop.service.PromotionService;
import com.clothingstore.shop.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/promotion-campaigns")
public class PromotionController {
    private final PromotionService promotions;

    public PromotionController(PromotionService promotions) {
        this.promotions = promotions;
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<Long>> create(HttpServletRequest request,
                                                        @RequestBody PromotionCampaignRequest body) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Promotion campaign created",
                promotions.create(body, token(request))));
    }

    @PostMapping("/{id}/revisions")
    public ResponseEntity<ApiResponseDTO<Integer>> revise(HttpServletRequest request, @PathVariable long id,
                                                           @RequestBody PromotionRevisionRequest body) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Promotion revision created",
                promotions.addRevision(id, body, token(request))));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponseDTO<PromotionCampaign>> publish(HttpServletRequest request, @PathVariable long id,
                                                                      @RequestBody PromotionPublishRequest body) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Promotion published",
                promotions.publish(id, body, token(request))));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiResponseDTO<PromotionCampaign>> pause(HttpServletRequest request, @PathVariable long id) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Promotion paused",
                promotions.pause(id, token(request))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<PromotionCampaign>> get(HttpServletRequest request, @PathVariable long id) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Promotion campaign fetched",
                promotions.get(id, token(request))));
    }

    @GetMapping("/{id}/revisions/{version}")
    public ResponseEntity<ApiResponseDTO<PromotionVersion>> getVersion(HttpServletRequest request,
                                                                        @PathVariable long id,
                                                                        @PathVariable int version) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Promotion revision fetched",
                promotions.getVersion(id, version, token(request))));
    }

    private String token(HttpServletRequest request) {
        String token = TokenUtils.extractTokenFromCookies(request);
        if (token == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token not found");
        return token;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDTO<Void>> badRequest(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(new ApiResponseDTO<>(false, error.getMessage(), null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponseDTO<Void>> conflict(IllegalStateException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponseDTO<>(false, error.getMessage(), null));
    }
}
