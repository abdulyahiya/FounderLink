package com.capgemini.user.controller;

import com.capgemini.user.dto.UserProfileRequest;
import com.capgemini.user.dto.UserProfileResponse;
import com.capgemini.user.service.UserProfileCommandService;
import com.capgemini.user.service.UserProfileQueryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserProfileController {

    private final UserProfileCommandService userProfileCommandService;
    private final UserProfileQueryService userProfileQueryService;

    public UserProfileController(UserProfileCommandService userProfileCommandService, UserProfileQueryService userProfileQueryService) {
        this.userProfileCommandService = userProfileCommandService;
        this.userProfileQueryService = userProfileQueryService;
    }

    @PostMapping("/profile")
    public ResponseEntity<UserProfileResponse> createProfile(
            Authentication authentication,
            @Valid @RequestBody UserProfileRequest request) {

        Long userId = Long.parseLong(authentication.getName());
        UserProfileResponse response = userProfileCommandService.createProfile(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getProfileByUserId(@PathVariable("id") Long userId) {
        try {
            UserProfileResponse response = userProfileQueryService.getProfileByUserId(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // No profile exists yet — return empty shell so the form renders with blank fields
            return ResponseEntity.ok(UserProfileResponse.builder().userId(userId).build());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("authentication.name == #id.toString() or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @PathVariable("id") Long id,
            @Valid @RequestBody UserProfileRequest request) {

        UserProfileResponse response = userProfileCommandService.updateProfile(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Page<UserProfileResponse>> getAllProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UserProfileResponse> profiles = userProfileQueryService.getAllProfiles(pageable);
        return ResponseEntity.ok(profiles);
    }

    // Search users by skill keyword — founders use this to find potential team members
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ROLE_FOUNDER') or hasAuthority('ROLE_COFOUNDER')")
    public ResponseEntity<List<UserProfileResponse>> searchBySkill(
            @RequestParam String skill) {
        List<UserProfileResponse> results = userProfileQueryService.searchBySkill(skill);
        return ResponseEntity.ok(results);
    }

    // Fetch profiles for a specific list of user IDs, with optional skill filter
    // Used after fetching co-founder IDs from AuthService
    @GetMapping("/profiles/batch")
    @PreAuthorize("hasAuthority('ROLE_FOUNDER') or hasAuthority('ROLE_COFOUNDER')")
    public ResponseEntity<List<UserProfileResponse>> getProfilesBatch(
            @RequestParam String userIds,
            @RequestParam(required = false) String skill) {

        List<Long> ids = Arrays.stream(userIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<UserProfileResponse> results = userProfileQueryService.getProfilesByUserIds(ids, skill);
        return ResponseEntity.ok(results);
    }
}
