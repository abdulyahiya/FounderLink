package com.capgemini.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitPolicyTest {

    @Test
    void resolve_whenLoginPost_shouldReturnLoginPolicy() {
        assertThat(RateLimitPolicy.resolve("/auth/login", "POST")).isEqualTo(RateLimitPolicy.LOGIN);
        assertThat(RateLimitPolicy.resolve("/auth/login/", "POST")).isEqualTo(RateLimitPolicy.LOGIN);
        assertThat(RateLimitPolicy.resolve("/AUTH/LOGIN", "POST")).isEqualTo(RateLimitPolicy.LOGIN);
        assertThat(RateLimitPolicy.resolve("/api/auth/login", "POST")).isEqualTo(RateLimitPolicy.LOGIN);
        assertThat(RateLimitPolicy.resolve("/auth/login?redirect=/dashboard", "POST")).isEqualTo(RateLimitPolicy.LOGIN);
    }

    @Test
    void resolve_whenLoginGet_shouldReturnGlobalPolicy() {
        // GET on login should not consume login burst limit
        assertThat(RateLimitPolicy.resolve("/auth/login", "GET")).isEqualTo(RateLimitPolicy.GLOBAL);
    }

    @Test
    void resolve_whenRegisterPost_shouldReturnRegisterPolicy() {
        assertThat(RateLimitPolicy.resolve("/auth/register", "POST")).isEqualTo(RateLimitPolicy.REGISTER);
        assertThat(RateLimitPolicy.resolve("/auth/register/", "POST")).isEqualTo(RateLimitPolicy.REGISTER);
        assertThat(RateLimitPolicy.resolve("/api/auth/register", "POST")).isEqualTo(RateLimitPolicy.REGISTER);
    }

    @Test
    void resolve_whenForgotPasswordPost_shouldReturnForgotPasswordPolicy() {
        assertThat(RateLimitPolicy.resolve("/auth/forgot-password", "POST")).isEqualTo(RateLimitPolicy.FORGOT_PASSWORD);
        assertThat(RateLimitPolicy.resolve("/auth/forgot-password/", "POST")).isEqualTo(RateLimitPolicy.FORGOT_PASSWORD);
    }

    @Test
    void resolve_whenResetPasswordPost_shouldReturnResetPasswordPolicy() {
        assertThat(RateLimitPolicy.resolve("/auth/reset-password", "POST")).isEqualTo(RateLimitPolicy.RESET_PASSWORD);
    }

    @Test
    void resolve_whenChangePasswordPost_shouldReturnChangePasswordPolicy() {
        assertThat(RateLimitPolicy.resolve("/auth/change-password", "POST")).isEqualTo(RateLimitPolicy.CHANGE_PASSWORD);
    }

    @Test
    void resolve_whenRefreshTokenPost_shouldReturnRefreshTokenPolicy() {
        assertThat(RateLimitPolicy.resolve("/auth/refresh", "POST")).isEqualTo(RateLimitPolicy.REFRESH_TOKEN);
        assertThat(RateLimitPolicy.resolve("/auth/refresh-token", "POST")).isEqualTo(RateLimitPolicy.REFRESH_TOKEN);
    }

    @Test
    void resolve_whenOtpPost_shouldReturnOtpPolicy() {
        assertThat(RateLimitPolicy.resolve("/auth/otp", "POST")).isEqualTo(RateLimitPolicy.OTP);
        assertThat(RateLimitPolicy.resolve("/auth/verify-otp", "POST")).isEqualTo(RateLimitPolicy.OTP);
    }

    @Test
    void resolve_whenPaymentPost_shouldReturnPaymentPolicy() {
        assertThat(RateLimitPolicy.resolve("/payments/create-order", "POST")).isEqualTo(RateLimitPolicy.PAYMENT);
        assertThat(RateLimitPolicy.resolve("/api/payments/verify", "POST")).isEqualTo(RateLimitPolicy.PAYMENT);
    }

    @Test
    void resolve_whenSubpathDoesNotMatchExact_shouldNotAccidentallyMatchLogin() {
        // Subpaths like /auth/login-history or /auth/login-status must not be treated as /auth/login
        assertThat(RateLimitPolicy.resolve("/auth/login-history", "POST")).isEqualTo(RateLimitPolicy.GLOBAL);
        assertThat(RateLimitPolicy.resolve("/auth/login_stats", "POST")).isEqualTo(RateLimitPolicy.GLOBAL);
    }

    @Test
    void resolve_whenNullOrEmptyUri_shouldReturnGlobalPolicy() {
        assertThat(RateLimitPolicy.resolve(null, "POST")).isEqualTo(RateLimitPolicy.GLOBAL);
        assertThat(RateLimitPolicy.resolve("", "POST")).isEqualTo(RateLimitPolicy.GLOBAL);
        assertThat(RateLimitPolicy.resolve("/auth/login", null)).isEqualTo(RateLimitPolicy.GLOBAL);
    }
}
