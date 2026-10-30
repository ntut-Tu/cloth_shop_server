package com.clothingstore.shop.service;

import com.clothingstore.shop.repository.RbacRepository;
import com.clothingstore.shop.security.Privilege;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {
    @Mock JwtService jwtService;
    @Mock RbacRepository repository;
    RbacService service;

    @BeforeEach
    void setUp() {
        service = new RbacService(jwtService, repository);
    }

    @Test
    void missingTokenIsUnauthorized() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requirePermission(null, Privilege.ADMIN_USERS_READ));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    @Test
    void invalidSignatureIsUnauthorized() {
        when(jwtService.extractUserId("bad")).thenThrow(new JwtException("bad signature"));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requirePermission("bad", Privilege.ADMIN_USERS_READ));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verify(repository, never()).hasPrivilege(7, Privilege.ADMIN_USERS_READ.name());
    }

    @Test
    void inactiveOrChangedAccountIsForbiddenBeforeGrantLookup() {
        tokenForActiveAccount(false);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requirePermission("jwt", Privilege.ADMIN_USERS_READ));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(repository, never()).hasPrivilege(7, Privilege.ADMIN_USERS_READ.name());
    }

    @Test
    void permissionRevocationTakesEffectOnTheNextRequest() {
        tokenForActiveAccount(true);
        when(repository.hasPrivilege(7, Privilege.ADMIN_USERS_READ.name())).thenReturn(true, false);
        assertEquals(7, service.requirePermission("jwt", Privilege.ADMIN_USERS_READ));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requirePermission("jwt", Privilege.ADMIN_USERS_READ));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void unknownPrivilegeCannotBePersisted() {
        manager();
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.createRole("jwt", "support", List.of("UNLISTED_PRIVILEGE")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repository, never()).createRole(anyString());
    }

    @Test
    void duplicateRoleIsConflict() {
        manager();
        when(repository.roleExists("SUPPORT")).thenReturn(true);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.createRole("jwt", "support", List.of("ADMIN_USERS_READ")));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void roleCreationNormalizesAndDeduplicatesPermissions() {
        manager();
        var role = service.createRole("jwt", "support", List.of("admin_users_read", "ADMIN_USERS_READ"));
        assertEquals("SUPPORT", role.name());
        assertEquals(Set.of("ADMIN_USERS_READ"), role.privileges());
        verify(repository).createRole("SUPPORT");
        verify(repository).replaceRolePrivileges("SUPPORT", Set.of("ADMIN_USERS_READ"));
    }

    @Test
    void systemRolePrivilegesCannotBeChanged() {
        manager();
        when(repository.roleExists("ADMIN")).thenReturn(true);
        when(repository.isSystemRole("ADMIN")).thenReturn(true);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.replacePrivileges("jwt", "ADMIN", List.of()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).replaceRolePrivileges("ADMIN", Set.of());
    }

    @Test
    void managerCannotRemoveOwnManagementRole() {
        manager();
        when(repository.userExists(7)).thenReturn(true);
        when(repository.roleExists("SUPPORT")).thenReturn(true);
        when(repository.rolePrivileges("SUPPORT")).thenReturn(Set.of("RBAC_MANAGE"));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.removeRole("jwt", 7, "SUPPORT"));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).removeRole(7, "SUPPORT");
    }

    @Test
    void managerCanAssignCustomRole() {
        manager();
        when(repository.userExists(9)).thenReturn(true);
        when(repository.roleExists("SUPPORT")).thenReturn(true);
        when(repository.userRoles(9)).thenReturn(Set.of("CUSTOMER", "SUPPORT"));
        assertEquals(Set.of("CUSTOMER", "SUPPORT"), service.assignRole("jwt", 9, "support"));
        verify(repository).assignRole(9, "SUPPORT");
    }

    private void manager() {
        tokenForActiveAccount(true);
        when(repository.hasPrivilege(7, Privilege.RBAC_MANAGE.name())).thenReturn(true);
    }

    private void tokenForActiveAccount(boolean active) {
        when(jwtService.extractUserId("jwt")).thenReturn(7);
        when(jwtService.extractRole("jwt")).thenReturn("admin");
        when(repository.isActiveUserWithType(7, "admin")).thenReturn(active);
    }
}
