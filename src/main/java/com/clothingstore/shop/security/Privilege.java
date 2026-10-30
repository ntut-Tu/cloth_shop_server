package com.clothingstore.shop.security;

/** Permission codes stored in rbac_privilege and resolved from the database on each request. */
public enum Privilege {
    RBAC_MANAGE,
    ADMIN_USERS_READ,
    ADMIN_USERS_BAN,
    ADMIN_LOGS_READ,
    PLATFORM_LEDGER_READ,
    ADMIN_PRODUCTS_READ,
    PRODUCT_STATUS_MANAGE,
    ADMIN_ORDERS_READ,
    ADMIN_REFUNDS_MANAGE
}
