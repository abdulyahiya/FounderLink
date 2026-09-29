package com.capgemini.team.controller;

import com.capgemini.team.config.JwtAuthenticationFilter;
import com.capgemini.team.config.JwtUtil;
import com.capgemini.team.config.SecurityConfig;
import com.capgemini.team.dto.InvitationRequest;
import com.capgemini.team.dto.InvitationResponse;
import com.capgemini.team.dto.RoleUpdateRequest;
import com.capgemini.team.dto.TeamMemberResponse;
import com.capgemini.team.enums.InvitationStatus;
import com.capgemini.team.enums.TeamRole;
import com.capgemini.team.service.TeamCommandService;
import com.capgemini.team.service.TeamQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
@Import(SecurityConfig.class)
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TeamCommandService teamCommandService;

    @MockitoBean
    private TeamQueryService teamQueryService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    private InvitationResponse sampleInvitationResponse;
    private TeamMemberResponse sampleMemberResponse;
    private InvitationRequest sampleInvitationRequest;

    @BeforeEach
    void setUp() throws Exception {
        // Make the mocked JWT filter pass through so requests reach the controller
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        sampleInvitationRequest = InvitationRequest.builder()
                .startupId(10L)
                .invitedUserId(2L)
                .role(TeamRole.CO_FOUNDER)
                .build();

        sampleInvitationResponse = InvitationResponse.builder()
                .id(50L)
                .startupId(10L)
                .invitedUserId(2L)
                .role(TeamRole.CO_FOUNDER)
                .status(InvitationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        sampleMemberResponse = TeamMemberResponse.builder()
                .id(200L)
                .startupId(10L)
                .userId(2L)
                .role(TeamRole.CO_FOUNDER)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /teams/invite
    // -------------------------------------------------------------------------

    @Test
    void inviteCoFounder_withFounderRole_shouldReturn201() throws Exception {
        // given
        UsernamePasswordAuthenticationToken founderAuth = new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_FOUNDER")));
        when(teamCommandService.inviteCoFounder(any(InvitationRequest.class), eq(1L)))
                .thenReturn(sampleInvitationResponse);

        // when / then
        mockMvc.perform(post("/teams/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleInvitationRequest))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(founderAuth))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void inviteCoFounder_withInvestorRole_shouldReturn403() throws Exception {
        // given — INVESTOR does not have ROLE_FOUNDER
        UsernamePasswordAuthenticationToken investorAuth = new UsernamePasswordAuthenticationToken(
                2L, null, List.of(new SimpleGrantedAuthority("ROLE_INVESTOR")));

        // when / then
        mockMvc.perform(post("/teams/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleInvitationRequest))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(investorAuth))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void inviteCoFounder_withoutAuth_shouldReturn401() throws Exception {
        // when / then — no authentication token provided
        mockMvc.perform(post("/teams/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleInvitationRequest))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // POST /teams/join/{invitationId}
    // -------------------------------------------------------------------------

    @Test
    void acceptInvitation_whenAuthenticated_shouldReturn200() throws Exception {
        // given
        UsernamePasswordAuthenticationToken userAuth = new UsernamePasswordAuthenticationToken(
                2L, null, List.of(new SimpleGrantedAuthority("ROLE_INVESTOR")));
        when(teamCommandService.acceptInvitation(eq(50L), eq(2L))).thenReturn(sampleMemberResponse);

        // when / then
        mockMvc.perform(post("/teams/join/50")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuth))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(200))
                .andExpect(jsonPath("$.userId").value(2));
    }

    // -------------------------------------------------------------------------
    // PUT /teams/reject/{invitationId}
    // -------------------------------------------------------------------------

    @Test
    void rejectInvitation_whenAuthenticated_shouldReturn200() throws Exception {
        // given
        UsernamePasswordAuthenticationToken userAuth = new UsernamePasswordAuthenticationToken(
                2L, null, List.of(new SimpleGrantedAuthority("ROLE_INVESTOR")));
        InvitationResponse rejectedResponse = InvitationResponse.builder()
                .id(50L)
                .startupId(10L)
                .invitedUserId(2L)
                .role(TeamRole.CO_FOUNDER)
                .status(InvitationStatus.REJECTED)
                .createdAt(LocalDateTime.now())
                .build();
        when(teamCommandService.rejectInvitation(eq(50L), eq(2L))).thenReturn(rejectedResponse);

        // when / then
        mockMvc.perform(put("/teams/reject/50")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuth))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    // -------------------------------------------------------------------------
    // PUT /teams/{memberId}/role
    // -------------------------------------------------------------------------

    @Test
    void updateMemberRole_withFounderRole_shouldReturn200() throws Exception {
        // given
        UsernamePasswordAuthenticationToken founderAuth = new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_FOUNDER")));
        RoleUpdateRequest roleUpdateRequest = RoleUpdateRequest.builder()
                .role(TeamRole.CTO)
                .build();
        TeamMemberResponse updatedMemberResponse = TeamMemberResponse.builder()
                .id(200L)
                .startupId(10L)
                .userId(2L)
                .role(TeamRole.CTO)
                .joinedAt(LocalDateTime.now())
                .build();
        when(teamCommandService.updateMemberRole(eq(200L), any(RoleUpdateRequest.class), eq(1L)))
                .thenReturn(updatedMemberResponse);

        // when / then
        mockMvc.perform(put("/teams/200/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleUpdateRequest))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(founderAuth))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CTO"));
    }

    // -------------------------------------------------------------------------
    // GET /teams/startup/{startupId}
    // -------------------------------------------------------------------------

    @Test
    void getTeamByStartup_shouldReturn200() throws Exception {
        // given
        UsernamePasswordAuthenticationToken userAuth = new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_FOUNDER")));
        when(teamQueryService.getTeamByStartup(10L)).thenReturn(List.of(sampleMemberResponse));

        // when / then
        mockMvc.perform(get("/teams/startup/10")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startupId").value(10));
    }

    // -------------------------------------------------------------------------
    // GET /teams/invitations/my
    // -------------------------------------------------------------------------

    @Test
    void getMyInvitations_whenAuthenticated_shouldReturn200() throws Exception {
        // given
        UsernamePasswordAuthenticationToken userAuth = new UsernamePasswordAuthenticationToken(
                2L, null, List.of(new SimpleGrantedAuthority("ROLE_INVESTOR")));
        when(teamQueryService.getMyInvitations(2L)).thenReturn(List.of(sampleInvitationResponse));

        // when / then
        mockMvc.perform(get("/teams/invitations/my")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(userAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invitedUserId").value(2));
    }
}
