package com.clothingstore.shop.exceptions;

import com.clothingstore.shop.enums.ExceptionCode;

public class SharedException extends Exception {
    private ExceptionCode exceptionCode;

    public SharedException(String message) {
        super(message);
    }

    public SharedException(String message, ExceptionCode errorCode) {
        super(message);
        this.exceptionCode = errorCode;
    }

    public SharedException(String message, Throwable cause) {
        super(message, cause);
    }

    public SharedException(String message, ExceptionCode exceptionCode, Throwable cause) {
        super(message, cause);
        this.exceptionCode = exceptionCode;
    }

    public ExceptionCode getErrorCode() {
        return exceptionCode;
    }
}