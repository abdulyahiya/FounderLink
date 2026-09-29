package com.capgemini.user.service;

import com.capgemini.user.dto.UserProfileRequest;
import com.capgemini.user.dto.UserProfileResponse;

public interface UserProfileCommandService {
    UserProfileResponse createProfile(Long userId, UserProfileRequest request);
    UserProfileResponse updateProfile(Long userId, UserProfileRequest request);
}
