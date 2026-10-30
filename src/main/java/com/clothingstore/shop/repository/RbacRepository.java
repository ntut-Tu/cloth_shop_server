package com.clothingstore.shop.repository;

import com.clothingstore.shop.security.RoleDefinition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Repository
public class RbacRepository {
    private final DSLContext dsl;

    public RbacRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public boolean isActiveUserWithType(int userId, String userType) {
        return dsl.fetchExists(dsl.selectOne().from("users")
                .where("user_id = ? AND user_type = ? AND is_active = true", userId, userType));
    }

    public boolean hasPrivilege(int userId, String privilege) {
        return dsl.fetchExists(dsl.selectOne()
                .from("rbac_user_role ur")
                .join("rbac_role_privilege rp").on("rp.role_name = ur.role_name")
                .where("ur.user_id = ? AND rp.privilege_code = ?", userId, privilege));
    }

    public List<String> listPrivileges() {
        return dsl.fetch("SELECT privilege_code FROM rbac_privilege ORDER BY privilege_code")
                .getValues("privilege_code", String.class);
    }

    public List<RoleDefinition> listRoles() {
        return dsl.fetch("SELECT role_name, system_role FROM rbac_role ORDER BY role_name")
                .map(row -> new RoleDefinition(row.get("role_name", String.class),
                        Boolean.TRUE.equals(row.get("system_role", Boolean.class)),
                        rolePrivileges(row.get("role_name", String.class))));
    }

    public Set<String> rolePrivileges(String roleName) {
        return new LinkedHashSet<>(dsl.fetch(
                "SELECT privilege_code FROM rbac_role_privilege WHERE role_name = ? ORDER BY privilege_code", roleName)
                .getValues("privilege_code", String.class));
    }

    public Set<String> userRoles(int userId) {
        return new LinkedHashSet<>(dsl.fetch(
                "SELECT role_name FROM rbac_user_role WHERE user_id = ? ORDER BY role_name", userId)
                .getValues("role_name", String.class));
    }

    public boolean userExists(int userId) {
        return dsl.fetchExists(dsl.selectOne().from("users").where("user_id = ?", userId));
    }

    public boolean roleExists(String roleName) {
        return dsl.fetchExists(dsl.selectOne().from("rbac_role")
                .where("role_name = ?", roleName));
    }

    public boolean isSystemRole(String roleName) {
        Record role = dsl.fetchOne("SELECT system_role FROM rbac_role WHERE role_name = ?", roleName);
        return role != null && Boolean.TRUE.equals(role.get("system_role", Boolean.class));
    }

    public void createRole(String roleName) {
        dsl.execute("INSERT INTO rbac_role(role_name, system_role) VALUES (?, false)", roleName);
    }

    public void replaceRolePrivileges(String roleName, Set<String> privileges) {
        dsl.execute("DELETE FROM rbac_role_privilege WHERE role_name = ?", roleName);
        for (String privilege : privileges) {
            dsl.execute("INSERT INTO rbac_role_privilege(role_name, privilege_code) VALUES (?, ?)",
                    roleName, privilege);
        }
    }

    public void assignRole(int userId, String roleName) {
        dsl.execute("INSERT INTO rbac_user_role(user_id, role_name) VALUES (?, ?) ON CONFLICT DO NOTHING",
                userId, roleName);
    }

    public void removeRole(int userId, String roleName) {
        dsl.execute("DELETE FROM rbac_user_role WHERE user_id = ? AND role_name = ?", userId, roleName);
    }
}
