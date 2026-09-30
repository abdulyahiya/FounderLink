package com.capgemini.authservice.controller;

import com.capgemini.authservice.mapper.AuthMapper;
import com.capgemini.authservice.service.IAuthService;
import com.capgemini.authservice.dto.*;
import com.capgemini.authservice.repository.UserRepository;
import com.capgemini.authservice.exception.CustomException;
import com.capgemini.authservice.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final JwtUtil jwtUtil;
    private final org.springframework.mail.javamail.JavaMailSender mailSender;
    private final com.capgemini.authservice.util.ResendEmailClient resendEmailClient;
    private final com.capgemini.authservice.util.BrevoEmailClient brevoEmailClient;

    @GetMapping("/test-email")
    public ResponseEntity<Map<String, Object>> testEmail(@RequestParam(defaultValue = "abdulyahya9973@gmail.com") String to) {
        Map<String, Object> res = new HashMap<>();
        String subject = "FounderLink Live Diagnostic Test";
        String html = "<h3>FounderLink Live Email Test</h3><p>If you see this email, HTTPS email delivery is active and working 100%!</p>";

        // 1. Try Brevo HTTPS
        if (brevoEmailClient.sendEmail(to, subject, html)) {
            res.put("success", true);
            res.put("provider", "Brevo HTTPS API");
            res.put("message", "Email delivered successfully via Brevo HTTPS to " + to);
            return ResponseEntity.ok(res);
        }

        // 2. Try Resend HTTPS
        if (resendEmailClient.sendEmail(to, subject, html)) {
            res.put("success", true);
            res.put("provider", "Resend HTTPS API");
            res.put("message", "Email delivered successfully via Resend HTTPS to " + to);
            return ResponseEntity.ok(res);
        }

        // 3. Try SMTP fallback
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("abdulyahya9973@gmail.com", "FounderLink");
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            res.put("success", true);
            res.put("provider", "SMTP");
            res.put("message", "Email sent successfully via SMTP to " + to);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("error", e.getMessage());
            if (e.getCause() != null) {
                res.put("cause", e.getCause().getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    // Returns a single user's summary (name, email, role) by their ID
    @GetMapping("/users/{id}")
    public ResponseEntity<UserSummaryDto> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok(authMapper.toUserSummaryDto(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Returns all users with the specified role — used by founders to find co-founders to invite
    @GetMapping("/users/by-role")
    public ResponseEntity<List<UserSummaryDto>> getUsersByRole(@RequestParam String role) {
        List<UserSummaryDto> users = userRepository.findByRolesName(role).stream()
                .map(authMapper::toUserSummaryDto)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Password reset link sent to your email", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        Long resolvedUserId = headerUserId;

        if (resolvedUserId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                resolvedUserId = jwtUtil.extractUserId(token);
            }
        }

        if (resolvedUserId == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                try {
                    resolvedUserId = Long.parseLong(auth.getName());
                } catch (NumberFormatException ignored) {}
            }
        }

        if (resolvedUserId == null) {
            throw new CustomException("Unauthorized: user ID could not be identified", HttpStatus.UNAUTHORIZED);
        }

        authService.changePassword(resolvedUserId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}
