package com.capgemini.authservice.service;

import com.capgemini.authservice.dto.AuthResponse;
import com.capgemini.authservice.dto.LoginRequest;
import com.capgemini.authservice.dto.RegisterRequest;
import com.capgemini.authservice.dto.RegisterResponse;
import com.capgemini.authservice.dto.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capgemini.authservice.entity.RoleEntity;
import com.capgemini.authservice.entity.UserEntity;
import com.capgemini.authservice.exception.CustomException;
import com.capgemini.authservice.repository.RoleRepository;
import com.capgemini.authservice.repository.UserRepository;
import com.capgemini.authservice.security.JwtUtil;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RabbitTemplate rabbitTemplate;
    private final WelcomeEmailService welcomeEmailService;
    private final PasswordResetEmailService passwordResetEmailService;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if ("ROLE_ADMIN".equalsIgnoreCase(request.getRole())) {
            throw new CustomException("Admin accounts cannot be self-registered", HttpStatus.FORBIDDEN);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("Email already registered", HttpStatus.CONFLICT);
        }

        RoleEntity role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new CustomException("Invalid role: " + request.getRole(), HttpStatus.BAD_REQUEST));

        UserEntity user = UserEntity.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(role))
                .build();

        UserEntity savedUser = userRepository.save(user);

        welcomeEmailService.sendWelcome(savedUser.getEmail(), savedUser.getName(), role.getName());

        try {
            rabbitTemplate.convertAndSend(exchange, "user.registered",
                    UserRegisteredEvent.builder()
                            .userId(savedUser.getId())
                            .name(savedUser.getName())
                            .email(savedUser.getEmail())
                            .role(role.getName())
                            .build());
            log.info("Published user.registered event for userId={}", savedUser.getId());
        } catch (Exception e) {
            log.warn("Failed to publish user.registered event for userId={}: {}", savedUser.getId(), e.getMessage());
        }

        return RegisterResponse.builder()
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(role.getName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        String roleName = user.getRoles().iterator().next().getName();

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), roleName);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(roleName)
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new CustomException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
        }

        String email = jwtUtil.extractEmail(refreshToken);
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        String roleName = user.getRoles().iterator().next().getName();

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), roleName);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(roleName)
                .build();
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found with email: " + email, HttpStatus.NOT_FOUND));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        passwordResetEmailService.sendResetLink(user.getEmail(), user.getName(), token);
        log.info("Password reset token generated and email sent for: {}", email);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        UserEntity user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new CustomException("Invalid or expired reset token", HttpStatus.BAD_REQUEST));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new CustomException("Reset token has expired", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new CustomException("Current password does not match", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed successfully for user: {}", user.getEmail());
    }
}
