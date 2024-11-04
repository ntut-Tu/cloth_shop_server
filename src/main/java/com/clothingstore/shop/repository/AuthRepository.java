package com.clothingstore.shop.repository;

import com.clothingstore.shop.model.AdminInfo;
import com.clothingstore.shop.model.CustomerInfo;
import com.clothingstore.shop.model.User;
import com.clothingstore.shop.model.VendorInfo;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static com.clothingstore.shop.jooq.tables.Users.USERS;
import static com.clothingstore.shop.jooq.tables.Vendor.VENDOR;
import static com.clothingstore.shop.jooq.tables.Customer.CUSTOMER;
import static com.clothingstore.shop.jooq.tables.Admin.ADMIN;

@Repository
public class AuthRepository {

    private final DSLContext dsl;

    @Autowired
    public AuthRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<User> findByUserAccount(String userAccount) {
        Record record = dsl.selectFrom(USERS)
                .where(USERS.ACCOUNT.eq(userAccount))
                .fetchOne();

        if (record == null) {
            return Optional.empty();
        }

        User user = mapToUser(record);

        switch (user.getUserType()) {
            case "vendor":
                user.setVendorInfo(loadVendorInfo(user.getId()));
                break;
            case "customer":
                user.setCustomerInfo(loadCustomerInfo(user.getId()));
                break;
            case "admin":
                user.setAdminInfo(new AdminInfo());
                break;
        }

        return Optional.of(user);
    }

    private User mapToUser(Record record) {
        return new User(
                record.get(USERS.USER_ID),
                record.get(USERS.ACCOUNT),
                record.get(USERS.PASSWORD),
                record.get(USERS.EMAIL),
                record.get(USERS.USER_TYPE),
                record.get(USERS.CREATED_AT),
                record.get(USERS.PHONE_NUMBER)
        );
    }

    private VendorInfo loadVendorInfo(int userId) {
        Record vendorRecord = dsl.selectFrom(VENDOR)
                .where(VENDOR.FK_USER_ID.eq(userId))
                .fetchOne();

        if (vendorRecord == null) return null;

        return new VendorInfo(
                vendorRecord.get(VENDOR.STORE_ADDRESS),
                vendorRecord.get(VENDOR.STORE_DESCRIPTION),
                vendorRecord.get(VENDOR.STORE_LOGO_URL),
                vendorRecord.get(VENDOR.PAYMENT_ACCOUNT),
                vendorRecord.get(VENDOR.IS_ACTIVE)
        );
    }

    private CustomerInfo loadCustomerInfo(int userId) {
        Record customerRecord = dsl.selectFrom(CUSTOMER)
                .where(CUSTOMER.FK_USER_ID.eq(userId))
                .fetchOne();

        if (customerRecord == null) return null;

        return new CustomerInfo(
                customerRecord.get(CUSTOMER.DEFAULT_SHIPPING_ADDRESS),
                customerRecord.get(CUSTOMER.BILLING_ADDRESS),
                customerRecord.get(CUSTOMER.IS_ACTIVE)
        );
    }
}
