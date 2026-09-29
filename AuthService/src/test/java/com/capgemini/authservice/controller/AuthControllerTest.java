package com.capgemini.authservice.controller;

import com.capgemini.authservice.dto.AuthResponse;
import com.capgemini.authservice.dto.RegisterResponse;
import com.capgemini.authservice.exception.CustomException;
import com.capgemini.authservice.exception.GlobalExceptionHandler;
import com.capgemini.authservice.mapper.AuthMapper;
import com.capgemini.authservice.repository.UserRepository;
import com.capgemini.authservice.security.SecurityConfig;
import com.capgemini.authservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthMapper authMapper;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // POST /auth/register
    // -------------------------------------------------------------------------

    @Test
    void register_withValidBody_shouldReturn201() throws Exception {
        // given
        Map<String, String> requestBody = Map.of(
                "name", "Alice",
                "email", "alice@example.com",
                "password", "password1",
                "role", "FOUNDER"
        );
        RegisterResponse registerResponse = RegisterResponse.builder()
                .userId(1L)
                .name("Alice")
                .email("alice@example.com")
                .role("FOUNDER")
                .build();
        when(authService.register(any())).thenReturn(registerResponse);

        // when / then
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.role").value("FOUNDER"));
    }

    @Test
    void register_withMissingFields_shouldReturn400() throws Exception {
        // given — body missing required fields (name, password, role are absent)
        Map<String, String> requestBody = Map.of("email", "not-a-valid-email");

        // when / then
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /auth/login
    // -------------------------------------------------------------------------

    @Test
    void login_withValidCredentials_shouldReturn200() throws Exception {
        // given
        Map<String, String> requestBody = Map.of(
                "email", "alice@example.com",
                "password", "password1"
        );
        AuthResponse authResponse = AuthResponse.builder()
                .token("access-token")
                .refreshToken("refresh-token")
                .userId(1L)
                .email("alice@example.com")
                .role("FOUNDER")
                .build();
        when(authService.login(any())).thenReturn(authResponse);

        // when / then
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.role").value("FOUNDER"));
    }

    @Test
    void login_withInvalidCredentials_shouldReturn401() throws Exception {
        // given
        Map<String, String> requestBody = Map.of(
                "email", "alice@example.com",
                "password", "wrong-password"
        );
        when(authService.login(any()))
                .thenThrow(new CustomException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        // when / then
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    // -------------------------------------------------------------------------
    // POST /auth/refresh
    // -------------------------------------------------------------------------

    @Test
    void refresh_withValidToken_shouldReturn200() throws Exception {
        // given
        Map<String, String> requestBody = Map.of("refreshToken", "valid.refresh.token");
        AuthResponse authResponse = AuthResponse.builder()
                .token("new-access-token")
                .refreshToken("new-refresh-token")
                .userId(1L)
                .email("alice@example.com")
                .role("FOUNDER")
                .build();
        when(authService.refreshToken("valid.refresh.token")).thenReturn(authResponse);

        // when / then
        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.token").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
    }

    @Test
    void refresh_whenTokenInvalid_shouldReturn401() throws Exception {
        // given
        Map<String, String> requestBody = Map.of("refreshToken", "invalid.refresh.token");
        when(authService.refreshToken("invalid.refresh.token"))
                .thenThrow(new CustomException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED));

        // when / then
        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }
}
