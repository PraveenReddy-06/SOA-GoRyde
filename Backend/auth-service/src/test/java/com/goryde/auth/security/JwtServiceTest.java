package com.goryde.auth.security;

import com.goryde.auth.model.Role;
import com.goryde.auth.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("a-secure-test-secret-with-at-least-32-characters", 60_000);

    @Test
    void generatedTokenContainsIdentityAndValidates() {
        User user = new User("Ada", "ada@example.com",
                new BCryptPasswordEncoder().encode("password123"), Role.DRIVER);

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("ada@example.com");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void malformedTokenIsRejected() {
        assertThatThrownBy(() -> jwtService.extractUsername("not-a-jwt"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void expiredTokenIsRejected() throws InterruptedException {
        JwtService shortLivedService = new JwtService(
                "a-secure-test-secret-with-at-least-32-characters", -1);
        User user = new User("Ada", "ada@example.com", "password", Role.PASSENGER);

        String token = shortLivedService.generateToken(user);

        assertThatThrownBy(() -> shortLivedService.extractUsername(token))
                .isInstanceOf(RuntimeException.class);
    }
}
