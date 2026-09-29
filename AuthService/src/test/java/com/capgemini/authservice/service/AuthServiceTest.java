package com.capgemini.authservice.service;

import com.capgemini.authservice.dto.AuthResponse;
import com.capgemini.authservice.dto.LoginRequest;
import com.capgemini.authservice.dto.RegisterRequest;
import com.capgemini.authservice.dto.RegisterResponse;
import com.capgemini.authservice.entity.RoleEntity;
import com.capgemini.authservice.entity.UserEntity;
import com.capgemini.authservice.exception.CustomException;
import com.capgemini.authservice.repository.RoleRepository;
import com.capgemini.authservice.repository.UserRepository;
import com.capgemini.authservice.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private WelcomeEmailService welcomeEmailService;

    @Mock
    private PasswordResetEmailService passwordResetEmailService;

    @InjectMocks
    private AuthService authService;

    private RoleEntity founderRole;
    private UserEntity savedUser;

    @BeforeEach
    void setUp() {
        founderRole = RoleEntity.builder()
                .id(1L)
                .name("FOUNDER")
                .build();

        savedUser = UserEntity.builder()
                .id(10L)
                .name("Alice")
                .email("alice@example.com")
                .password("hashed-password")
                .roles(Set.of(founderRole))
                .build();
    }

    // -------------------------------------------------------------------------
    // register()
    // -------------------------------------------------------------------------

    @Test
    void register_whenEmailAlreadyExists_shouldThrowCustomException() {
        // given
        RegisterRequest request = new RegisterRequest("Alice", "alice@example.com", "password1", "FOUNDER");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Email already registered")
                .satisfies(ex -> assertThat(((CustomException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void register_whenRoleNotFound_shouldThrowCustomException() {
        // given
        RegisterRequest request = new RegisterRequest("Alice", "alice@example.com", "password1", "UNKNOWN_ROLE");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.findByName("UNKNOWN_ROLE")).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Invalid role")
                .satisfies(ex -> assertThat(((CustomException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void register_whenValidRequest_shouldReturnRegisterResponse() {
        // given
        RegisterRequest request = new RegisterRequest("Alice", "alice@example.com", "password1", "FOUNDER");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.findByName("FOUNDER")).thenReturn(Optional.of(founderRole));
        when(passwordEncoder.encode("password1")).thenReturn("hashed-password");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        // when
        RegisterResponse response = authService.register(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getRole()).isEqualTo("FOUNDER");
    }

    // -------------------------------------------------------------------------
    // login()
    // -------------------------------------------------------------------------

    @Test
    void login_whenEmailNotFound_shouldThrowCustomException() {
        // given
        LoginRequest request = new LoginRequest("unknown@example.com", "password1");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Invalid email or password")
                .satisfies(ex -> assertThat(((CustomException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void login_whenPasswordDoesNotMatch_shouldThrowCustomException() {
        // given
        LoginRequest request = new LoginRequest("alice@example.com", "wrong-password");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Invalid email or password")
                .satisfies(ex -> assertThat(((CustomException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void login_whenValidCredentials_shouldReturnAuthResponse() {
        // given
        LoginRequest request = new LoginRequest("alice@example.com", "password1");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password1", "hashed-password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(10L, "alice@example.com", "FOUNDER")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(10L, "alice@example.com")).thenReturn("refresh-token");

        // when
        AuthResponse response = authService.login(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getRole()).isEqualTo("FOUNDER");
    }

    // -------------------------------------------------------------------------
    // refreshToken()
    // -------------------------------------------------------------------------

    @Test
    void refreshToken_whenTokenInvalid_shouldThrowCustomException() {
        // given
        String invalidToken = "invalid.refresh.token";
        when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Invalid or expired refresh token")
                .satisfies(ex -> assertThat(((CustomException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void refreshToken_whenTokenValid_shouldReturnNewAuthResponse() {
        // given
        String validRefreshToken = "valid.refresh.token";
        when(jwtUtil.validateToken(validRefreshToken)).thenReturn(true);
        when(jwtUtil.extractEmail(validRefreshToken)).thenReturn("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(savedUser));
        when(jwtUtil.generateAccessToken(10L, "alice@example.com", "FOUNDER")).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(10L, "alice@example.com")).thenReturn("new-refresh-token");

        // when
        AuthResponse response = authService.refreshToken(validRefreshToken);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getRole()).isEqualTo("FOUNDER");
    }
}
