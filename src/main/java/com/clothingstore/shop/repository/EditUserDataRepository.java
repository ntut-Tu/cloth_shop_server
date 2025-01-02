package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.response.editUserData.EditUserDataResponseDTO;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static com.clothingstore.shop.jooq.Tables.USERS;

@Repository
public class EditUserDataRepository {
    private final DSLContext dsl;

    public EditUserDataRepository(DSLContext dsl) {
        this.dsl = dsl;
    }
    // edit user data
    public EditUserDataResponseDTO editUserData(Integer userID, String password, String email, String phone_number, String profile_pic_url) throws Exception {
        // try to update user data by user_id and return updated user data without user_id
        try {
            dsl.update(USERS)
                    .set(USERS.PASSWORD, password)
                    .set(USERS.EMAIL, email)
                    .set(USERS.PHONE_NUMBER, phone_number)
                    .set(USERS.PROFILE_PIC_URL, profile_pic_url)
                    .where(USERS.USER_ID.eq(userID))
                    .execute();
            return dsl.select(USERS.ACCOUNT, USERS.PASSWORD, USERS.EMAIL, USERS.PHONE_NUMBER, USERS.PROFILE_PIC_URL)
                    .from(USERS)
                    .where(USERS.USER_ID.eq(userID))
                    .fetchOneInto(EditUserDataResponseDTO.class);
        }catch (Exception e){
            throw e;
        }
    }
    // get user data
    public EditUserDataResponseDTO getUserData(Integer userId) throws Exception {
        try {
            return dsl.select(
                            USERS.ACCOUNT.as("username"),
                            USERS.PASSWORD,
                            USERS.EMAIL,
                            USERS.PHONE_NUMBER,
                            USERS.PROFILE_PIC_URL
                    )
                    .from(USERS)
                    .where(USERS.USER_ID.eq(userId))
                    .fetchOneInto(EditUserDataResponseDTO.class);
        }catch (Exception e){
            throw e;
        }
    }
}
