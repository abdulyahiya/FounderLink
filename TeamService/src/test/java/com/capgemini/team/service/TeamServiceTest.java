package com.capgemini.team.service;

import com.capgemini.team.dto.InvitationRequest;
import com.capgemini.team.dto.InvitationResponse;
import com.capgemini.team.dto.RoleUpdateRequest;
import com.capgemini.team.dto.TeamMemberResponse;
import com.capgemini.team.entity.TeamInvitation;
import com.capgemini.team.entity.TeamMember;
import com.capgemini.team.enums.InvitationStatus;
import com.capgemini.team.enums.TeamRole;
import com.capgemini.team.event.EventPublisher;
import com.capgemini.team.exception.BadRequestException;
import com.capgemini.team.exception.DuplicateResourceException;
import com.capgemini.team.exception.ResourceNotFoundException;
import com.capgemini.team.exception.UnauthorizedException;
import com.capgemini.team.feign.StartupClient;
import com.capgemini.team.feign.StartupDTO;
import com.capgemini.team.feign.UserClient;
import com.capgemini.team.feign.UserDTO;
import com.capgemini.team.mapper.TeamMapper;
import com.capgemini.team.repository.TeamInvitationRepository;
import com.capgemini.team.repository.TeamMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamServiceTest {

    @Mock
    private TeamInvitationRepository invitationRepository;

    @Mock
    private TeamMemberRepository memberRepository;

    @Mock
    private StartupClient startupClient;

    @Mock
    private UserClient userClient;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private org.springframework.cloud.client.circuitbreaker.CircuitBreaker startupCircuitBreaker;

    @Mock
    private org.springframework.cloud.client.circuitbreaker.CircuitBreaker userCircuitBreaker;

    @Mock
    private TeamMapper teamMapper;

    @InjectMocks
    private TeamService teamService;

    private StartupDTO sampleStartup;
    private UserDTO sampleUser;
    private TeamInvitation sampleInvitation;
    private TeamMember sampleMember;

    @BeforeEach
    void setUp() {
        when(circuitBreakerFactory.create("startup-service")).thenReturn(startupCircuitBreaker);
        when(circuitBreakerFactory.create("user-service")).thenReturn(userCircuitBreaker);

        when(startupCircuitBreaker.run(any(), any())).thenAnswer(invocation -> {
            Supplier<?> s = invocation.getArgument(0);
            return s.get();
        });
        when(userCircuitBreaker.run(any(), any())).thenAnswer(invocation -> {
            Supplier<?> s = invocation.getArgument(0);
            return s.get();
        });

        sampleStartup = StartupDTO.builder()
                .id(10L)
                .name("TechStartup")
                .founderId(1L)
                .build();

        sampleUser = UserDTO.builder()
                .id(2L)
                .name("Jane Doe")
                .email("jane@example.com")
                .build();

        sampleInvitation = TeamInvitation.builder()
                .id(50L)
                .startupId(10L)
                .invitedUserId(2L)
                .role(TeamRole.CO_FOUNDER)
                .status(InvitationStatus.PENDING)
                .build();
        sampleInvitation.setCreatedAt(LocalDateTime.now());

        sampleMember = TeamMember.builder()
                .id(200L)
                .startupId(10L)
                .userId(2L)
                .role(TeamRole.CO_FOUNDER)
                .build();
        sampleMember.setJoinedAt(LocalDateTime.now());

        // Default mapper stubs (lenient — only used when the mapper is called)
        when(teamMapper.toInvitationResponse(any(TeamInvitation.class))).thenAnswer(inv -> {
            TeamInvitation inv1 = inv.getArgument(0);
            return InvitationResponse.builder()
                    .id(inv1.getId()).startupId(inv1.getStartupId())
                    .invitedUserId(inv1.getInvitedUserId())
                    .role(inv1.getRole()).status(inv1.getStatus())
                    .createdAt(inv1.getCreatedAt()).build();
        });
        when(teamMapper.toMemberResponse(any(TeamMember.class))).thenAnswer(inv -> {
            TeamMember m = inv.getArgument(0);
            return TeamMemberResponse.builder()
                    .id(m.getId()).startupId(m.getStartupId())
                    .userId(m.getUserId()).role(m.getRole())
                    .joinedAt(m.getJoinedAt()).build();
        });
    }

    // -------------------------------------------------------------------------
    // inviteCoFounder
    // -------------------------------------------------------------------------

    @Test
    void inviteCoFounder_whenNotFounder_shouldThrowUnauthorizedException() {
        // given
        InvitationRequest request = InvitationRequest.builder()
                .startupId(10L)
                .invitedUserId(2L)
                .role(TeamRole.CO_FOUNDER)
                .build();
        when(startupClient.getStartupById(10L)).thenReturn(sampleStartup); // founderId = 1L

        // when / then — pass a different founderId
        assertThatThrownBy(() -> teamService.inviteCoFounder(request, 99L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("founder");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    void inviteCoFounder_whenDuplicatePendingInvitation_shouldThrowDuplicateResourceException() {
        // given
        InvitationRequest request = InvitationRequest.builder()
                .startupId(10L)
                .invitedUserId(2L)
                .role(TeamRole.CO_FOUNDER)
                .build();
        when(startupClient.getStartupById(10L)).thenReturn(sampleStartup);
        when(userClient.getUserById(2L)).thenReturn(sampleUser);
        when(invitationRepository.findByStartupIdAndInvitedUserIdAndStatus(10L, 2L, InvitationStatus.PENDING))
                .thenReturn(Optional.of(sampleInvitation));

        // when / then
        assertThatThrownBy(() -> teamService.inviteCoFounder(request, 1L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("pending invitation");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    void inviteCoFounder_whenValid_shouldSaveAndReturnInvitationResponse() {
        // given
        InvitationRequest request = InvitationRequest.builder()
                .startupId(10L)
                .invitedUserId(2L)
                .role(TeamRole.CO_FOUNDER)
                .build();
        when(startupClient.getStartupById(10L)).thenReturn(sampleStartup);
        when(userClient.getUserById(2L)).thenReturn(sampleUser);
        when(invitationRepository.findByStartupIdAndInvitedUserIdAndStatus(10L, 2L, InvitationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(invitationRepository.save(any(TeamInvitation.class))).thenReturn(sampleInvitation);
        doNothing().when(eventPublisher).publishTeamInviteSent(any());

        // when
        InvitationResponse response = teamService.inviteCoFounder(request, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStartupId()).isEqualTo(10L);
        assertThat(response.getInvitedUserId()).isEqualTo(2L);
        assertThat(response.getStatus()).isEqualTo(InvitationStatus.PENDING);
        verify(invitationRepository).save(any(TeamInvitation.class));
        verify(eventPublisher).publishTeamInviteSent(any());
    }

    // -------------------------------------------------------------------------
    // acceptInvitation
    // -------------------------------------------------------------------------

    @Test
    void acceptInvitation_whenNotForThisUser_shouldThrowUnauthorizedException() {
        // given
        when(invitationRepository.findById(50L)).thenReturn(Optional.of(sampleInvitation)); // invitedUserId = 2L

        // when / then — pass a different userId
        assertThatThrownBy(() -> teamService.acceptInvitation(50L, 99L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("invitation");

        verify(memberRepository, never()).save(any());
    }

    @Test
    void acceptInvitation_whenNotPending_shouldThrowBadRequestException() {
        // given
        TeamInvitation acceptedInvitation = TeamInvitation.builder()
                .id(50L)
                .startupId(10L)
                .invitedUserId(2L)
                .role(TeamRole.CO_FOUNDER)
                .status(InvitationStatus.ACCEPTED)
                .build();
        when(invitationRepository.findById(50L)).thenReturn(Optional.of(acceptedInvitation));

        // when / then
        assertThatThrownBy(() -> teamService.acceptInvitation(50L, 2L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PENDING");

        verify(memberRepository, never()).save(any());
    }

    @Test
    void acceptInvitation_whenValid_shouldCreateTeamMember() {
        // given
        when(invitationRepository.findById(50L)).thenReturn(Optional.of(sampleInvitation));
        when(invitationRepository.save(any(TeamInvitation.class))).thenReturn(sampleInvitation);
        when(memberRepository.save(any(TeamMember.class))).thenReturn(sampleMember);

        // when
        TeamMemberResponse response = teamService.acceptInvitation(50L, 2L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(2L);
        assertThat(response.getStartupId()).isEqualTo(10L);
        assertThat(response.getRole()).isEqualTo(TeamRole.CO_FOUNDER);
        verify(invitationRepository).save(any(TeamInvitation.class));
        verify(memberRepository).save(any(TeamMember.class));
    }

    // -------------------------------------------------------------------------
    // rejectInvitation
    // -------------------------------------------------------------------------

    @Test
    void rejectInvitation_whenValid_shouldSetRejectedStatus() {
        // given
        when(invitationRepository.findById(50L)).thenReturn(Optional.of(sampleInvitation));
        TeamInvitation rejectedInvitation = TeamInvitation.builder()
                .id(50L)
                .startupId(10L)
                .invitedUserId(2L)
                .role(TeamRole.CO_FOUNDER)
                .status(InvitationStatus.REJECTED)
                .build();
        when(invitationRepository.save(any(TeamInvitation.class))).thenReturn(rejectedInvitation);

        // when
        InvitationResponse response = teamService.rejectInvitation(50L, 2L);

        // then
        assertThat(response.getStatus()).isEqualTo(InvitationStatus.REJECTED);
        verify(invitationRepository).save(any(TeamInvitation.class));
    }

    // -------------------------------------------------------------------------
    // updateMemberRole
    // -------------------------------------------------------------------------

    @Test
    void updateMemberRole_whenNotFounder_shouldThrowUnauthorizedException() {
        // given
        when(memberRepository.findById(200L)).thenReturn(Optional.of(sampleMember)); // startupId = 10L
        when(startupClient.getStartupById(10L)).thenReturn(sampleStartup); // founderId = 1L
        RoleUpdateRequest request = RoleUpdateRequest.builder().role(TeamRole.CTO).build();

        // when / then — pass a different founderId
        assertThatThrownBy(() -> teamService.updateMemberRole(200L, request, 99L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("founder");

        verify(memberRepository, never()).save(any());
    }

    @Test
    void updateMemberRole_whenValid_shouldUpdateRole() {
        // given
        when(memberRepository.findById(200L)).thenReturn(Optional.of(sampleMember));
        when(startupClient.getStartupById(10L)).thenReturn(sampleStartup); // founderId = 1L
        TeamMember updatedMember = TeamMember.builder()
                .id(200L)
                .startupId(10L)
                .userId(2L)
                .role(TeamRole.CTO)
                .joinedAt(sampleMember.getJoinedAt())
                .build();
        when(memberRepository.save(any(TeamMember.class))).thenReturn(updatedMember);
        RoleUpdateRequest request = RoleUpdateRequest.builder().role(TeamRole.CTO).build();

        // when
        TeamMemberResponse response = teamService.updateMemberRole(200L, request, 1L);

        // then
        assertThat(response.getRole()).isEqualTo(TeamRole.CTO);
        verify(memberRepository).save(any(TeamMember.class));
    }

    // -------------------------------------------------------------------------
    // getMyInvitations
    // -------------------------------------------------------------------------

    @Test
    void getMyInvitations_shouldReturnInvitations() {
        // given
        when(invitationRepository.findByInvitedUserIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(sampleInvitation));

        // when
        List<InvitationResponse> result = teamService.getMyInvitations(2L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvitedUserId()).isEqualTo(2L);
        verify(invitationRepository).findByInvitedUserIdOrderByCreatedAtDesc(2L);
    }
}
