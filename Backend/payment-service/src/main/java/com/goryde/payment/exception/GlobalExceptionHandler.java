package com.goryde.payment.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(PaymentNotFoundException.class)
    ResponseEntity<Map<String, Object>> handleNotFound(PaymentNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    ResponseEntity<Map<String, Object>> handleDuplicate(DuplicatePaymentException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), null);
    }

    @ExceptionHandler({PaymentProcessingException.class, IllegalArgumentException.class})
    ResponseEntity<Map<String, Object>> handleProcessing(RuntimeException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), null);
    }

    @ExceptionHandler(RideServiceUnavailableException.class)
    ResponseEntity<Map<String, Object>> handleRideServiceUnavailable(RideServiceUnavailableException exception) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), null);
    }

    @ExceptionHandler(UnauthorizedPaymentAccessException.class)
    ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedPaymentAccessException exception) {
        return response(HttpStatus.FORBIDDEN, exception.getMessage(), null);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            ConstraintViolationException.class})
    ResponseEntity<Map<String, Object>> handleInvalidRequest(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException validationException) {
            Map<String, String> errors = new LinkedHashMap<>();
            validationException.getBindingResult().getFieldErrors()
                    .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
            return response(HttpStatus.BAD_REQUEST, "Validation failed", errors);
        }
        return response(HttpStatus.BAD_REQUEST, "Invalid request", null);
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message, Object details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (details != null) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}
