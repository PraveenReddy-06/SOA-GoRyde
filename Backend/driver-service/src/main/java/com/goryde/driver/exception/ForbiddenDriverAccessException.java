package com.goryde.driver.exception;

public class ForbiddenDriverAccessException extends RuntimeException {
    public ForbiddenDriverAccessException(String message) {
        super(message);
    }
}
