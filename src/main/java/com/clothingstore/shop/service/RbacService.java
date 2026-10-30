package com.clothingstore.shop.service;

import com.clothingstore.shop.repository.RbacRepository;
import com.clothingstore.shop.security.Privilege;
import com.clothingstore.shop.security.RoleDefinition;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RbacService {
    private final JwtService jwtService;
    private final RbacRepository repository;

    public RbacService(JwtService jwtService, RbacRepository repository) {
        this.jwtService = jwtService;
        this.repository = repository;
    }

    public int requirePermission(String token, Privilege privilege) {
        int userId = requireActiveUser(token);
        if (!repository.hasPrivilege(userId, privilege.name())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing privilege: " + privilege.name());
        }
        return userId;
    }

    public int requireActiveUser(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        final int userId;
        final String userType;
        try {
            Integer claimId = jwtService.extractUserId(token);
            userType = jwtService.extractRole(token);
            if (claimId == null || userType == null) {
                throw new IllegalArgumentException("Incomplete token");
            }
            userId = claimId;
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token", e);
        }
        if (!repository.isActiveUserWithType(userId, userType)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is inactive or its type has changed");
        }
        return userId;
    }

    public List<String> listPrivileges(String token) {
        requirePermission(token, Privilege.RBAC_MANAGE);
        return repository.listPrivileges();
    }

    public List<RoleDefinition> listRoles(String token) {
        requirePermission(token, Privilege.RBAC_MANAGE);
        return repository.listRoles();
    }

    public Set<String> userRoles(String token, int userId) {
        requirePermission(token, Privilege.RBAC_MANAGE);
        requireUser(userId);
        return repository.userRoles(userId);
    }

    @Transactional
    public RoleDefinition createRole(String token, String roleName, List<String> privilegeCodes) {
        requirePermission(token, Privilege.RBAC_MANAGE);
        String name = normalizeRoleName(roleName);
        Set<String> privileges = normalizePrivileges(privilegeCodes);
        if (repository.roleExists(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role already exists");
        }
        repository.createRole(name);
        repository.replaceRolePrivileges(name, privileges);
        return new RoleDefinition(name, false, privileges);
    }

    @Transactional
    public RoleDefinition replacePrivileges(String token, String roleName, List<String> privilegeCodes) {
        requirePermission(token, Privilege.RBAC_MANAGE);
        String name = normalizeRoleName(roleName);
        requireMutableRole(name);
        Set<String> privileges = normalizePrivileges(privilegeCodes);
        repository.replaceRolePrivileges(name, privileges);
        return new RoleDefinition(name, false, privileges);
    }

    @Transactional
    public Set<String> assignRole(String token, int userId, String roleName) {
        requirePermission(token, Privilege.RBAC_MANAGE);
        requireUser(userId);
        String name = normalizeRoleName(roleName);
        requireMutableRole(name);
        repository.assignRole(userId, name);
        return repository.userRoles(userId);
    }

    @Transactional
    public Set<String> removeRole(String token, int userId, String roleName) {
        int actorId = requirePermission(token, Privilege.RBAC_MANAGE);
        requireUser(userId);
        String name = normalizeRoleName(roleName);
        requireMutableRole(name);
        if (actorId == userId && repository.rolePrivileges(name).contains(Privilege.RBAC_MANAGE.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot remove your own management role");
        }
        repository.removeRole(userId, name);
        return repository.userRoles(userId);
    }

    private void requireUser(int userId) {
        if (userId <= 0 || !repository.userExists(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    private void requireMutableRole(String roleName) {
        if (!repository.roleExists(roleName)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
        }
        if (repository.isSystemRole(roleName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "System roles are immutable");
        }
    }

    private String normalizeRoleName(String roleName) {
        String name = roleName == null ? "" : roleName.trim().toUpperCase(Locale.ROOT);
        if (!name.matches("[A-Z][A-Z0-9_]{2,39}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role name");
        }
        return name;
    }

    private Set<String> normalizePrivileges(List<String> privilegeCodes) {
        if (privilegeCodes == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Privileges are required");
        }
        Set<String> known = Arrays.stream(Privilege.values()).map(Enum::name).collect(Collectors.toSet());
        Set<String> result = new LinkedHashSet<>();
        for (String code : privilegeCodes) {
            String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
            if (!known.contains(normalized)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown privilege: " + code);
            }
            result.add(normalized);
        }
        return result;
    }
}
