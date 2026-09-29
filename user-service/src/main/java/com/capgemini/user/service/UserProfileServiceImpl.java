package com.capgemini.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capgemini.user.dto.UserProfileRequest;
import com.capgemini.user.dto.UserProfileResponse;
import com.capgemini.user.entity.UserProfile;
import com.capgemini.user.exception.DuplicateResourceException;
import com.capgemini.user.exception.ResourceNotFoundException;
import com.capgemini.user.mapper.UserProfileMapper;
import com.capgemini.user.repository.UserProfileRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

    public UserProfileServiceImpl(UserProfileRepository userProfileRepository, UserProfileMapper userProfileMapper) {
        this.userProfileRepository = userProfileRepository;
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "userProfiles", key = "#userId"),
        @CacheEvict(value = "userSkillSearch", allEntries = true)
    })
    public UserProfileResponse createProfile(Long userId, UserProfileRequest request) {
        if (userProfileRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException("Profile already exists for user ID: " + userId);
        }
        if (userProfileRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .name(request.getName())
                .email(request.getEmail())
                .bio(request.getBio())
                .skills(request.getSkills())
                .experience(request.getExperience())
                .portfolioLinks(request.getPortfolioLinks())
                .build();

        UserProfile saved = userProfileRepository.save(profile);
        log.info("User profile created: userId={}", userId);
        return userProfileMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userProfiles", key = "#userId")
    public UserProfileResponse getProfileByUserId(Long userId) {
        log.info("Fetching user profile from DB: userId={}", userId);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user ID: " + userId));
        return userProfileMapper.toResponse(profile);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "userProfiles", key = "#userId"),
        @CacheEvict(value = "userSkillSearch", allEntries = true)
    })
    public UserProfileResponse updateProfile(Long userId, UserProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        if (profile == null) {
            // Profile doesn't exist yet (e.g. admin) — create it on first save
            if (userProfileRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Email already in use: " + request.getEmail());
            }
            profile = UserProfile.builder()
                    .userId(userId)
                    .name(request.getName())
                    .email(request.getEmail())
                    .bio(request.getBio())
                    .skills(request.getSkills())
                    .experience(request.getExperience())
                    .portfolioLinks(request.getPortfolioLinks())
                    .build();
            UserProfile created = userProfileRepository.save(profile);
            log.info("User profile created on first save: userId={}", userId);
            return userProfileMapper.toResponse(created);
        }

        userProfileRepository.findByEmail(request.getEmail())
                .ifPresent(existing -> {
                    if (!existing.getUserId().equals(userId)) {
                        throw new DuplicateResourceException("Email already in use: " + request.getEmail());
                    }
                });

        profile.setName(request.getName());
        profile.setEmail(request.getEmail());
        profile.setBio(request.getBio());
        profile.setSkills(request.getSkills());
        profile.setExperience(request.getExperience());
        profile.setPortfolioLinks(request.getPortfolioLinks());

        UserProfile updated = userProfileRepository.save(profile);
        log.info("User profile updated: userId={}", userId);
        return userProfileMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllProfiles(Pageable pageable) {
        return userProfileRepository.findAll(pageable).map(userProfileMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userSkillSearch", key = "#keyword")
    public List<UserProfileResponse> searchBySkill(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return userProfileRepository.findBySkillsContaining(keyword.trim())
                .stream()
                .map(userProfileMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getProfilesByUserIds(List<Long> userIds, String skill) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<UserProfile> profiles;
        if (skill != null && !skill.trim().isEmpty()) {
            profiles = userProfileRepository.findByUserIdInAndSkillsContaining(userIds, skill.trim());
        } else {
            profiles = userProfileRepository.findByUserIdIn(userIds);
        }
        return profiles.stream().map(userProfileMapper::toResponse).collect(Collectors.toList());
    }


}
