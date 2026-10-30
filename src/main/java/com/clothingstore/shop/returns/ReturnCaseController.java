package com.clothingstore.shop.returns;

import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.service.JwtService;
import com.clothingstore.shop.utils.TokenUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/return-cases")
public class ReturnCaseController {
    private final ReturnCaseService service;
    private final JwtService jwtService;

    public ReturnCaseController(ReturnCaseService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<ReturnCaseModels.Detail>> create(
            HttpServletRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestBody ReturnCaseModels.CreateRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponseDTO<>(true, "Return case created",
                service.create(actor(request), body, key)));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<ReturnCaseModels.Summary>>> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Return cases retrieved",
                service.list(actor(request), page, size)));
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<ApiResponseDTO<ReturnCaseModels.Detail>> get(
            HttpServletRequest request, @PathVariable UUID caseId) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Return case retrieved", service.get(actor(request), caseId)));
    }

    @PostMapping("/{caseId}/vendor-decision")
    public ResponseEntity<ApiResponseDTO<ReturnCaseModels.Detail>> vendorDecision(
            HttpServletRequest request, @PathVariable UUID caseId,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestBody ReturnCaseModels.DecisionRequest body) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Vendor decision recorded",
                service.vendorDecision(actor(request), caseId, body, key)));
    }

    @PostMapping("/{caseId}/appeal")
    public ResponseEntity<ApiResponseDTO<ReturnCaseModels.Detail>> appeal(
            HttpServletRequest request, @PathVariable UUID caseId,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestBody ReturnCaseModels.AppealRequest body) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Return appeal recorded",
                service.appeal(actor(request), caseId, body, key)));
    }

    @PostMapping("/{caseId}/admin-decision")
    public ResponseEntity<ApiResponseDTO<ReturnCaseModels.Detail>> adminDecision(
            HttpServletRequest request, @PathVariable UUID caseId,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestBody ReturnCaseModels.DecisionRequest body) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Admin decision recorded",
                service.adminDecision(actor(request), caseId, body, key)));
    }

    @PostMapping("/{caseId}/withdraw")
    public ResponseEntity<ApiResponseDTO<ReturnCaseModels.Detail>> withdraw(
            HttpServletRequest request, @PathVariable UUID caseId,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Return case withdrawn",
                service.withdraw(actor(request), caseId, key)));
    }

    private ReturnCaseService.Actor actor(HttpServletRequest request) {
        String token = TokenUtils.extractTokenFromCookies(request);
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            String role = jwtService.extractRole(token);
            Integer userId = jwtService.extractUserId(token);
            if (role == null || userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
            return new ReturnCaseService.Actor(userId, role);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }
}
