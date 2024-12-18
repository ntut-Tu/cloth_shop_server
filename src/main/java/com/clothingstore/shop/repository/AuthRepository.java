package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.request.auth.RegisterRequestDTO;
import com.clothingstore.shop.dto.repository.users.AdminInfoRepositoryDTO;
import com.clothingstore.shop.dto.repository.users.CustomerInfoRepositoryDTO;
import com.clothingstore.shop.dto.repository.users.UserRepositoryDTO;
import com.clothingstore.shop.dto.repository.users.VendorInfoRepositoryDTO;
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

    public Optional<UserRepositoryDTO> findByUserAccount(String userAccount, String role) {
        Optional<UserRepositoryDTO> userOptional = findByUserAccountAndRole(userAccount, role);

        userOptional.ifPresent(user -> {
            switch (user.getUserType()) {
                case "vendor":
                    user.setVendorInfo(loadVendorInfo(user, user.getId()));
                    break;
                case "customer":
                    user.setCustomerInfo(loadCustomerInfo(user, user.getId()));
                    break;
                case "admin":
                    user.setAdminInfo(new AdminInfoRepositoryDTO());
                    break;
            }
        });

        return userOptional;
    }


    private UserRepositoryDTO mapToUser(Record record) {
        return new UserRepositoryDTO(
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

    private VendorInfoRepositoryDTO loadVendorInfo(UserRepositoryDTO userRepositoryDTO, Integer userId) {
        Record vendorRecord = dsl.selectFrom(VENDOR)
                .where(VENDOR.FK_USER_ID.eq(userId))
                .fetchOne();

        if (vendorRecord == null) return null;

        return new VendorInfoRepositoryDTO(
                userRepositoryDTO,
                vendorRecord.get(VENDOR.STORE_ADDRESS),
                vendorRecord.get(VENDOR.STORE_DESCRIPTION),
                vendorRecord.get(VENDOR.STORE_LOGO_URL),
                vendorRecord.get(VENDOR.PAYMENT_ACCOUNT)
        );
    }

    private CustomerInfoRepositoryDTO loadCustomerInfo(UserRepositoryDTO userRepositoryDTO, Integer userId) {
        Record customerRecord = dsl.selectFrom(CUSTOMER)
                .where(CUSTOMER.FK_USER_ID.eq(userId))
                .fetchOne();

        if (customerRecord == null) return null;

        return new CustomerInfoRepositoryDTO(
                userRepositoryDTO,
                customerRecord.get(CUSTOMER.DEFAULT_SHIPPING_ADDRESS),
                customerRecord.get(CUSTOMER.BILLING_ADDRESS)
        );
    }

    public Optional<UserRepositoryDTO> findByUserAccountAndRole(String userAccount, String role) {
        Record record = dsl.selectFrom(USERS)
                .where(USERS.ACCOUNT.eq(userAccount))
                .and(USERS.USER_TYPE.eq(role))
                .fetchOne();

        return record != null ? Optional.of(mapToUser(record)) : Optional.empty();
    }

    public Optional<UserRepositoryDTO> findByUserIDAndRole(Integer user_id, String role) {
        Record record = dsl.selectFrom(USERS)
                .where(USERS.USER_ID.eq(user_id))
                .and(USERS.USER_TYPE.eq(role))
                .fetchOne();

        return record != null ? Optional.of(mapToUser(record)) : Optional.empty();
    }

    public boolean saveUser(UserRepositoryDTO userRepositoryDTO, RegisterRequestDTO registerRequestDTO) {
        int userId = dsl.insertInto(USERS)
                .set(USERS.ACCOUNT, userRepositoryDTO.getAccount())
                .set(USERS.PASSWORD, userRepositoryDTO.getPassword())
                .set(USERS.EMAIL, userRepositoryDTO.getEmail())
                .set(USERS.USER_TYPE, userRepositoryDTO.getUserType())
                .set(USERS.CREATED_AT, userRepositoryDTO.getCreatedAt())
                .set(USERS.IS_ACTIVE, userRepositoryDTO.getIsActive())
                .returning(USERS.USER_ID)
                .fetchOne()
                .getValue(USERS.USER_ID);

        switch (userRepositoryDTO.getUserType()) {
            case "vendor":
                dsl.insertInto(VENDOR)
                        .set(VENDOR.FK_USER_ID, userId)
                        .set(VENDOR.STORE_ADDRESS, registerRequestDTO.getStoreAddress())
                        .set(VENDOR.STORE_DESCRIPTION, registerRequestDTO.getStoreDescription())
                        .set(VENDOR.STORE_LOGO_URL, registerRequestDTO.getStoreLogoUrl())
                        .set(VENDOR.PAYMENT_ACCOUNT, registerRequestDTO.getPaymentAccount())
                        .execute();
                break;
            case "customer":
                dsl.insertInto(CUSTOMER)
                        .set(CUSTOMER.FK_USER_ID, userId)
                        .set(CUSTOMER.DEFAULT_SHIPPING_ADDRESS, registerRequestDTO.getDefaultShippingAddress())
                        .set(CUSTOMER.BILLING_ADDRESS, registerRequestDTO.getBillingAddress())
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

    public Integer getVendorId(Integer userId) {
        return dsl.select(VENDOR.VENDOR_ID)
                .from(VENDOR)
                .where(VENDOR.FK_USER_ID.eq(userId))
                .fetchOne()
                .get(VENDOR.VENDOR_ID);
    }

    public Integer getAdminId(Integer userId) {
        return dsl.select(ADMIN.ADMIN_ID)
                .from(ADMIN)
                .where(ADMIN.FK_USER_ID.eq(userId))
                .fetchOne()
                .get(ADMIN.ADMIN_ID);
    }
}
