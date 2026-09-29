package com.capgemini.user.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String bio;
    private String skills;
    private String experience;
    private String portfolioLinks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}