package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.RegisterRequest;
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

    public Optional<User> findByUserAccount(String userAccount, String role) {
        Optional<User> userOptional = findByUserAccountAndRole(userAccount, role);

        userOptional.ifPresent(user -> {
            switch (user.getUserType()) {
                case "vendor":
                    user.setVendorInfo(loadVendorInfo(user, user.getId()));
                    break;
                case "customer":
                    user.setCustomerInfo(loadCustomerInfo(user, user.getId()));
                    break;
                case "admin":
                    user.setAdminInfo(new AdminInfo());
                    break;
            }
        });

        return userOptional;
    }


    private User mapToUser(Record record) {
        return new User(
                record.get(USERS.USER_ID),
                record.get(USERS.ACCOUNT),
                record.get(USERS.PASSWORD),
                record.get(USERS.EMAIL),
                record.get(USERS.IS_ACTIVE),
                record.get(USERS.USER_TYPE),
                record.get(USERS.CREATED_AT),
                record.get(USERS.PHONE_NUMBER)
        );
    }

    private VendorInfo loadVendorInfo(User user,Integer userId) {
        Record vendorRecord = dsl.selectFrom(VENDOR)
                .where(VENDOR.FK_USER_ID.eq(userId))
                .fetchOne();

        if (vendorRecord == null) return null;

        return new VendorInfo(
                user,
                vendorRecord.get(VENDOR.STORE_ADDRESS),
                vendorRecord.get(VENDOR.STORE_DESCRIPTION),
                vendorRecord.get(VENDOR.STORE_LOGO_URL),
                vendorRecord.get(VENDOR.PAYMENT_ACCOUNT)
        );
    }

    private CustomerInfo loadCustomerInfo(User user,Integer userId) {
        Record customerRecord = dsl.selectFrom(CUSTOMER)
                .where(CUSTOMER.FK_USER_ID.eq(userId))
                .fetchOne();

        if (customerRecord == null) return null;

        return new CustomerInfo(
                user,
                customerRecord.get(CUSTOMER.DEFAULT_SHIPPING_ADDRESS),
                customerRecord.get(CUSTOMER.BILLING_ADDRESS)
        );
    }

    public Optional<User> findByUserAccountAndRole(String userAccount, String role) {
        Record record = dsl.selectFrom(USERS)
                .where(USERS.ACCOUNT.eq(userAccount))
                .and(USERS.USER_TYPE.eq(role))
                .fetchOne();

        return record != null ? Optional.of(mapToUser(record)) : Optional.empty();
    }

    public boolean saveUser(User user, RegisterRequest registerRequest) {
        int userId = dsl.insertInto(USERS)
                .set(USERS.ACCOUNT, user.getAccount())
                .set(USERS.PASSWORD, user.getPassword())
                .set(USERS.EMAIL, user.getEmail())
                .set(USERS.USER_TYPE, user.getUserType())
                .set(USERS.CREATED_AT, user.getCreatedAt())
                .set(USERS.IS_ACTIVE, user.getIsActive())
                .returning(USERS.USER_ID)
                .fetchOne()
                .getValue(USERS.USER_ID);

        switch (user.getUserType()) {
            case "vendor":
                dsl.insertInto(VENDOR)
                        .set(VENDOR.FK_USER_ID, userId)
                        .set(VENDOR.STORE_ADDRESS, registerRequest.getStoreAddress())
                        .set(VENDOR.STORE_DESCRIPTION, registerRequest.getStoreDescription())
                        .set(VENDOR.STORE_LOGO_URL, registerRequest.getStoreLogoUrl())
                        .set(VENDOR.PAYMENT_ACCOUNT, registerRequest.getPaymentAccount())
                        .execute();
                break;
            case "customer":
                dsl.insertInto(CUSTOMER)
                        .set(CUSTOMER.FK_USER_ID, userId)
                        .set(CUSTOMER.DEFAULT_SHIPPING_ADDRESS, registerRequest.getDefaultShippingAddress())
                        .set(CUSTOMER.BILLING_ADDRESS, registerRequest.getBillingAddress())
                        .execute();
                break;
            case "admin":
                dsl.insertInto(ADMIN)
                        .set(ADMIN.FK_USER_ID, userId)
                        .execute();
                break;
        }

        return true;
    }
}
