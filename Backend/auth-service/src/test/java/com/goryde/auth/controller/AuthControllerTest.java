package com.goryde.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goryde.auth.dto.AuthResponse;
import com.goryde.auth.dto.UserResponse;
import com.goryde.auth.model.Role;
import com.goryde.auth.model.User;
import com.goryde.auth.repository.UserRepository;
import com.goryde.auth.security.JwtService;
import com.goryde.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void meReturnsAuthenticatedUserWithoutPassword() throws Exception {
        User user = new User("Ada", "ada@example.com", "hashed-password", Role.PASSENGER);
        when(authService.register(any())).thenReturn(new UserResponse(null, "Ada", "ada@example.com", Role.PASSENGER));
        var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        mockMvc.perform(get("/api/auth/me").with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.role").value("PASSENGER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
