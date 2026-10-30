package com.clothingstore.shop.controller;

import com.clothingstore.shop.dto.response.ApiResponseDTO;
import com.clothingstore.shop.security.RoleDefinition;
import com.clothingstore.shop.service.RbacService;
import com.clothingstore.shop.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/rbac")
public class RbacController {
    private final RbacService rbacService;

    public RbacController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping("/privileges")
    public ApiResponseDTO<List<String>> privileges(HttpServletRequest request) {
        return new ApiResponseDTO<>(true, "Privileges fetched", rbacService.listPrivileges(token(request)));
    }

    @GetMapping("/roles")
    public ApiResponseDTO<List<RoleDefinition>> roles(HttpServletRequest request) {
        return new ApiResponseDTO<>(true, "Roles fetched", rbacService.listRoles(token(request)));
    }

    @GetMapping("/users/{userId}/roles")
    public ApiResponseDTO<Set<String>> userRoles(HttpServletRequest request, @PathVariable int userId) {
        return new ApiResponseDTO<>(true, "User roles fetched", rbacService.userRoles(token(request), userId));
    }

    @PostMapping("/roles")
    public ApiResponseDTO<RoleDefinition> createRole(HttpServletRequest request, @RequestBody RoleRequest body) {
        return new ApiResponseDTO<>(true, "Role created",
                rbacService.createRole(token(request), body.name(), body.privileges()));
    }

    @PutMapping("/roles/{roleName}/privileges")
    public ApiResponseDTO<RoleDefinition> replacePrivileges(HttpServletRequest request,
                                                              @PathVariable String roleName,
                                                              @RequestBody PrivilegesRequest body) {
        return new ApiResponseDTO<>(true, "Role privileges replaced",
                rbacService.replacePrivileges(token(request), roleName, body.privileges()));
    }

    @PutMapping("/users/{userId}/roles/{roleName}")
    public ApiResponseDTO<Set<String>> assignRole(HttpServletRequest request, @PathVariable int userId,
                                                   @PathVariable String roleName) {
        return new ApiResponseDTO<>(true, "Role assigned", rbacService.assignRole(token(request), userId, roleName));
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    public ApiResponseDTO<Set<String>> removeRole(HttpServletRequest request, @PathVariable int userId,
                                                   @PathVariable String roleName) {
        return new ApiResponseDTO<>(true, "Role removed", rbacService.removeRole(token(request), userId, roleName));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleAccessError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode())
                .body(new ApiResponseDTO<>(false, error.getReason(), null));
    }

    private String token(HttpServletRequest request) {
        return TokenUtils.extractTokenFromCookies(request);
    }

    public record RoleRequest(String name, List<String> privileges) { }
    public record PrivilegesRequest(List<String> privileges) { }
}
