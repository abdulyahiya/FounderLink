package com.capgemini.authservice.mapper;

import com.capgemini.authservice.dto.UserSummaryDto;
import com.capgemini.authservice.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public UserSummaryDto toUserSummaryDto(UserEntity user) {
        String role = user.getRoles().stream()
                .findFirst()
                .map(r -> r.getName())
                .orElse(null);
        return UserSummaryDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(role)
                .build();
    }
}
