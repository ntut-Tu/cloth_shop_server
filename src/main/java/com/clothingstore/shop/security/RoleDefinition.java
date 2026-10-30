package com.clothingstore.shop.security;

import java.util.Set;

public record RoleDefinition(String name, boolean systemRole, Set<String> privileges) {
}
