package com.capgemini.startup.controller;

import com.capgemini.startup.dto.StartupRequest;
import com.capgemini.startup.dto.StartupResponse;
import com.capgemini.startup.enums.StartupStage;
import com.capgemini.startup.filter.JwtAuthenticationFilter;
import com.capgemini.startup.filter.JwtUtil;
import com.capgemini.startup.service.StartupCommandService;
import com.capgemini.startup.service.StartupQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StartupController.class)
@Import(StartupControllerTest.MethodSecurityConfig.class)
class StartupControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StartupCommandService startupCommandService;

    @MockitoBean
    private StartupQueryService startupQueryService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    // Principals are Strings because the controller calls authentication.getName()
    private UsernamePasswordAuthenticationToken founderAuth;
    private UsernamePasswordAuthenticationToken investorAuth;
    private UsernamePasswordAuthenticationToken adminAuth;

    private StartupRequest startupRequest;
    private StartupResponse startupResponse;

    @BeforeEach
    void setUp() throws Exception {
        // Make the mocked JWT filter pass through so requests reach the controller
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        founderAuth = new UsernamePasswordAuthenticationToken(
                "1", null, List.of(new SimpleGrantedAuthority("ROLE_FOUNDER")));
        investorAuth = new UsernamePasswordAuthenticationToken(
                "2", null, List.of(new SimpleGrantedAuthority("ROLE_INVESTOR")));
        adminAuth = new UsernamePasswordAuthenticationToken(
                "3", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        startupRequest = StartupRequest.builder()
                .name("GreenTech")
                .description("Eco-friendly solutions")
                .industry("CleanTech")
                .problemStatement("Carbon emissions")
                .solution("Carbon capture tech")
                .fundingGoal(new BigDecimal("500000.00"))
                .stage(StartupStage.IDEA)
                .location("Berlin")
                .build();

        startupResponse = StartupResponse.builder()
                .id(1L)
                .name("GreenTech")
                .description("Eco-friendly solutions")
                .industry("CleanTech")
                .problemStatement("Carbon emissions")
                .solution("Carbon capture tech")
                .fundingGoal(new BigDecimal("500000.00"))
                .stage(StartupStage.IDEA)
                .location("Berlin")
                .founderId(1L)
                .isApproved(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // -----------------------------------------------------------------------
    // POST /startups
    // -----------------------------------------------------------------------

    @Test
    void createStartup_withFounderRole_shouldReturn201() throws Exception {
        // given
        when(startupCommandService.createStartup(eq(1L), any(StartupRequest.class))).thenReturn(startupResponse);

        // when / then
        mockMvc.perform(post("/startups")
                        .with(authentication(founderAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("GreenTech"))
                .andExpect(jsonPath("$.founderId").value(1));
    }

    @Test
    void createStartup_withInvestorRole_shouldReturn403() throws Exception {
        // when / then
        mockMvc.perform(post("/startups")
                        .with(authentication(investorAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startupRequest)))
                .andExpect(status().isForbidden());

        verify(startupCommandService, never()).createStartup(any(), any());
    }

    @Test
    void createStartup_withoutAuth_shouldReturn401() throws Exception {
        // when / then
        mockMvc.perform(post("/startups")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startupRequest)))
                .andExpect(status().isUnauthorized());

        verify(startupCommandService, never()).createStartup(any(), any());
    }

    // -----------------------------------------------------------------------
    // GET /startups
    // -----------------------------------------------------------------------

    @Test
    void getAllApprovedStartups_shouldReturn200() throws Exception {
        // given
        Page<StartupResponse> page = new PageImpl<>(
                List.of(startupResponse), PageRequest.of(0, 10), 1);
        when(startupQueryService.getAllApprovedStartups(any())).thenReturn(page);

        // when / then
        mockMvc.perform(get("/startups")
                        .with(authentication(founderAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("GreenTech"));
    }

    // -----------------------------------------------------------------------
    // GET /startups/{id}
    // -----------------------------------------------------------------------

    @Test
    void getStartupById_shouldReturn200() throws Exception {
        // given
        when(startupQueryService.getStartupById(1L)).thenReturn(startupResponse);

        // when / then
        mockMvc.perform(get("/startups/1")
                        .with(authentication(founderAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("GreenTech"));
    }

    // -----------------------------------------------------------------------
    // PUT /startups/{id}
    // -----------------------------------------------------------------------

    @Test
    void updateStartup_withFounderRole_shouldReturn200() throws Exception {
        // given
        StartupResponse updatedResponse = StartupResponse.builder()
                .id(1L)
                .name("GreenTech Updated")
                .industry("CleanTech")
                .fundingGoal(new BigDecimal("750000.00"))
                .stage(StartupStage.MVP)
                .founderId(1L)
                .isApproved(false)
                .build();
        when(startupCommandService.updateStartup(eq(1L), eq(1L), any(StartupRequest.class))).thenReturn(updatedResponse);

        // when / then
        mockMvc.perform(put("/startups/1")
                        .with(authentication(founderAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("GreenTech Updated"));
    }

    // -----------------------------------------------------------------------
    // DELETE /startups/{id}
    // -----------------------------------------------------------------------

    @Test
    void deleteStartup_withFounderRole_shouldReturn200() throws Exception {
        // given
        doNothing().when(startupCommandService).deleteStartup(eq(1L), eq(1L), eq(false));

        // when / then
        mockMvc.perform(delete("/startups/1")
                        .with(authentication(founderAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Startup deleted successfully"));
    }

    // -----------------------------------------------------------------------
    // POST /startups/{id}/follow
    // -----------------------------------------------------------------------

    @Test
    void followStartup_withInvestorRole_shouldReturn200() throws Exception {
        // given
        doNothing().when(startupCommandService).followStartup(eq(1L), eq(2L));

        // when / then
        mockMvc.perform(post("/startups/1/follow")
                        .with(authentication(investorAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Startup followed successfully"));
    }

    // -----------------------------------------------------------------------
    // PUT /startups/{id}/approve
    // -----------------------------------------------------------------------

    @Test
    void approveStartup_withAdminRole_shouldReturn200() throws Exception {
        // given
        StartupResponse approvedResponse = StartupResponse.builder()
                .id(1L)
                .name("GreenTech")
                .industry("CleanTech")
                .fundingGoal(new BigDecimal("500000.00"))
                .stage(StartupStage.IDEA)
                .founderId(1L)
                .isApproved(true)
                .build();
        when(startupCommandService.approveStartup(1L)).thenReturn(approvedResponse);

        // when / then
        mockMvc.perform(put("/startups/1/approve")
                        .with(authentication(adminAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isApproved").value(true));
    }

    @Test
    void approveStartup_withFounderRole_shouldReturn403() throws Exception {
        // when / then
        mockMvc.perform(put("/startups/1/approve")
                        .with(authentication(founderAuth))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());

        verify(startupCommandService, never()).approveStartup(any());
    }
}
