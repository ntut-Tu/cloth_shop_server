package com.clothingstore.shop.utils;

public class EnumUtils {
    public static boolean isStringInEnum(String value, Class<? extends Enum<?>> enumClass) {
        for (Enum<?> enumValue : enumClass.getEnumConstants()) {
            if (enumValue.toString().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
