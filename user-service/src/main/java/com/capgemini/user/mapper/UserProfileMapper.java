package com.capgemini.user.mapper;

import com.capgemini.user.dto.UserProfileResponse;
import com.capgemini.user.entity.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public UserProfileResponse toResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .name(profile.getName())
                .email(profile.getEmail())
                .bio(profile.getBio())
                .skills(profile.getSkills())
                .experience(profile.getExperience())
                .portfolioLinks(profile.getPortfolioLinks())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
