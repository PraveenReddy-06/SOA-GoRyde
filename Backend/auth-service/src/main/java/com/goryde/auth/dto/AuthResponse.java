package com.goryde.auth.dto;

import com.goryde.auth.model.Role;
import com.goryde.auth.model.User;

public record AuthResponse(
        String token,
        Long id,
        String name,
        String email,
        Role role
) {
    public static AuthResponse from(String token, User user) {
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
