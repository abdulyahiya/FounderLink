package com.capgemini.user.service;

import com.capgemini.user.dto.UserProfileRequest;
import com.capgemini.user.dto.UserProfileResponse;
import com.capgemini.user.entity.UserProfile;
import com.capgemini.user.exception.DuplicateResourceException;
import com.capgemini.user.exception.ResourceNotFoundException;
import com.capgemini.user.mapper.UserProfileMapper;
import com.capgemini.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfileMapper userProfileMapper;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private UserProfileRequest request;
    private UserProfile profile;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        request = UserProfileRequest.builder()
                .name("Alice Founder")
                .email("alice@example.com")
                .bio("Building the future")
                .skills("Java, Spring")
                .experience("5 years")
                .portfolioLinks("https://alice.dev")
                .build();

        profile = UserProfile.builder()
                .id(10L)
                .userId(USER_ID)
                .name("Alice Founder")
                .email("alice@example.com")
                .bio("Building the future")
                .skills("Java, Spring")
                .experience("5 years")
                .portfolioLinks("https://alice.dev")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── createProfile ──────────────────────────────────────────────────────────

    @Test
    void createProfile_whenProfileAlreadyExists_shouldThrowDuplicateResourceException() {
        // given
        given(userProfileRepository.existsByUserId(USER_ID)).willReturn(true);

        // when / then
        assertThatThrownBy(() -> userProfileService.createProfile(USER_ID, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(USER_ID.toString());

        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void createProfile_whenEmailInUse_shouldThrowDuplicateResourceException() {
        // given
        given(userProfileRepository.existsByUserId(USER_ID)).willReturn(false);
        given(userProfileRepository.existsByEmail(request.getEmail())).willReturn(true);

        // when / then
        assertThatThrownBy(() -> userProfileService.createProfile(USER_ID, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(request.getEmail());

        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void createProfile_whenValid_shouldReturnUserProfileResponse() {
        // given
        given(userProfileRepository.existsByUserId(USER_ID)).willReturn(false);
        given(userProfileRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userProfileRepository.save(any(UserProfile.class))).willReturn(profile);
        UserProfileResponse mapped = UserProfileResponse.builder()
                .userId(USER_ID).name(profile.getName()).email(profile.getEmail())
                .bio(profile.getBio()).skills(profile.getSkills())
                .experience(profile.getExperience()).portfolioLinks(profile.getPortfolioLinks())
                .build();
        given(userProfileMapper.toResponse(profile)).willReturn(mapped);

        // when
        UserProfileResponse response = userProfileService.createProfile(USER_ID, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getName()).isEqualTo(request.getName());
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getBio()).isEqualTo(request.getBio());
        assertThat(response.getSkills()).isEqualTo(request.getSkills());
        assertThat(response.getExperience()).isEqualTo(request.getExperience());
        assertThat(response.getPortfolioLinks()).isEqualTo(request.getPortfolioLinks());
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    // ── getProfileByUserId ─────────────────────────────────────────────────────

    @Test
    void getProfileByUserId_whenNotFound_shouldThrowResourceNotFoundException() {
        // given
        given(userProfileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> userProfileService.getProfileByUserId(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(USER_ID.toString());
    }

    @Test
    void getProfileByUserId_whenFound_shouldReturnUserProfileResponse() {
        // given
        given(userProfileRepository.findByUserId(USER_ID)).willReturn(Optional.of(profile));
        UserProfileResponse mapped = UserProfileResponse.builder()
                .id(profile.getId()).userId(USER_ID).email(profile.getEmail()).build();
        given(userProfileMapper.toResponse(profile)).willReturn(mapped);

        // when
        UserProfileResponse response = userProfileService.getProfileByUserId(USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getEmail()).isEqualTo(profile.getEmail());
        assertThat(response.getId()).isEqualTo(profile.getId());
    }

    // ── updateProfile ──────────────────────────────────────────────────────────

    @Test
    void updateProfile_whenNotFound_shouldCreateNewProfile() {
        // given
        given(userProfileRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
        given(userProfileRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userProfileRepository.save(any(UserProfile.class))).willAnswer(inv -> inv.getArgument(0));
        given(userProfileMapper.toResponse(any(UserProfile.class)))
                .willReturn(UserProfileResponse.builder().userId(USER_ID).name(request.getName()).build());

        // when
        UserProfileResponse response = userProfileService.updateProfile(USER_ID, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void updateProfile_whenEmailTakenByAnother_shouldThrowDuplicateResourceException() {
        // given – email belongs to a different user (userId = 99)
        UserProfile otherUser = UserProfile.builder()
                .id(99L)
                .userId(99L)
                .email(request.getEmail())
                .build();

        given(userProfileRepository.findByUserId(USER_ID)).willReturn(Optional.of(profile));
        given(userProfileRepository.findByEmail(request.getEmail())).willReturn(Optional.of(otherUser));

        // when / then
        assertThatThrownBy(() -> userProfileService.updateProfile(USER_ID, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(request.getEmail());

        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void updateProfile_whenValid_shouldReturnUpdatedProfile() {
        // given
        UserProfileRequest updateRequest = UserProfileRequest.builder()
                .name("Alice Updated")
                .email("alice@example.com")
                .bio("Updated bio")
                .skills("Java, Kotlin")
                .experience("6 years")
                .portfolioLinks("https://alice.io")
                .build();

        UserProfile updatedProfile = UserProfile.builder()
                .id(10L)
                .userId(USER_ID)
                .name("Alice Updated")
                .email("alice@example.com")
                .bio("Updated bio")
                .skills("Java, Kotlin")
                .experience("6 years")
                .portfolioLinks("https://alice.io")
                .createdAt(profile.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        given(userProfileRepository.findByUserId(USER_ID)).willReturn(Optional.of(profile));
        // email belongs to the same user — no conflict
        given(userProfileRepository.findByEmail(updateRequest.getEmail())).willReturn(Optional.of(profile));
        given(userProfileRepository.save(any(UserProfile.class))).willReturn(updatedProfile);
        UserProfileResponse mapped = UserProfileResponse.builder()
                .name("Alice Updated").bio("Updated bio").skills("Java, Kotlin").build();
        given(userProfileMapper.toResponse(updatedProfile)).willReturn(mapped);

        // when
        UserProfileResponse response = userProfileService.updateProfile(USER_ID, updateRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Alice Updated");
        assertThat(response.getBio()).isEqualTo("Updated bio");
        assertThat(response.getSkills()).isEqualTo("Java, Kotlin");
        verify(userProfileRepository).save(any(UserProfile.class));
    }
}
