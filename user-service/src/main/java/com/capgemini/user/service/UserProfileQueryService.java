package com.capgemini.user.service;

import com.capgemini.user.dto.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserProfileQueryService {
    UserProfileResponse getProfileByUserId(Long userId);
    Page<UserProfileResponse> getAllProfiles(Pageable pageable);
    List<UserProfileResponse> searchBySkill(String keyword);
    List<UserProfileResponse> getProfilesByUserIds(List<Long> userIds, String skill);
}
