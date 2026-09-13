package com.goryde.driver.exception;

public class NoAvailableDriverException extends RuntimeException {
    public NoAvailableDriverException() {
        super("No available driver was found");
    }
}
