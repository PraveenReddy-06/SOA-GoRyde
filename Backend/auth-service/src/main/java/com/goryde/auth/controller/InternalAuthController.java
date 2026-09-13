package com.goryde.auth.controller;

import com.goryde.auth.dto.JwtValidationResponse;
import com.goryde.auth.model.User;
import com.goryde.auth.repository.UserRepository;
import com.goryde.auth.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public InternalAuthController(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/validate")
    public JwtValidationResponse validate(@RequestBody String token) {
        try {
            String username = jwtService.extractUsername(token);
            User user = userRepository.findByEmailIgnoreCase(username)
                    .filter(found -> jwtService.isTokenValid(token, found))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            return new JwtValidationResponse(user.getId(), user.getEmail(), user.getRole().name());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }
}