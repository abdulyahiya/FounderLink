package com.capgemini.authservice.service;

import com.capgemini.authservice.dto.AuthResponse;
import com.capgemini.authservice.dto.LoginRequest;
import com.capgemini.authservice.dto.RegisterRequest;
import com.capgemini.authservice.dto.RegisterResponse;

public interface IAuthService {
    RegisterResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
    void changePassword(Long userId, String oldPassword, String newPassword);
}