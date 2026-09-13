package com.goryde.ride.exception;

public class InvalidDriverResponseException extends RuntimeException {
    public InvalidDriverResponseException() {
        super("Driver Service returned invalid driver information");
    }
}
