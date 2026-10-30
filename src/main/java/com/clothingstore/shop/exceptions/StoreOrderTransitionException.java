package com.clothingstore.shop.exceptions;

import org.springframework.http.HttpStatus;

public class StoreOrderTransitionException extends RuntimeException {
    private final HttpStatus status;

    public StoreOrderTransitionException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
