package com.goryde.gateway.security;

public record AuthValidationResponse(Long userId, String email, String role) {
}
