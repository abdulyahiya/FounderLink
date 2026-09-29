package com.capgemini.investment.controller;

import com.capgemini.investment.config.JwtAuthenticationFilter;
import com.capgemini.investment.config.JwtUtil;
import com.capgemini.investment.config.SecurityConfig;
import com.capgemini.investment.dto.InvestmentRequest;
import com.capgemini.investment.dto.InvestmentResponse;
import com.capgemini.investment.enums.InvestmentStatus;
import com.capgemini.investment.service.InvestmentCommandService;
import com.capgemini.investment.service.InvestmentQueryService;
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

import java.math.BigDecimal;
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

@WebMvcTest(InvestmentController.class)
@Import(SecurityConfig.class)
class InvestmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InvestmentCommandService investmentCommandService;

    @MockitoBean
    private InvestmentQueryService investmentQueryService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    private InvestmentResponse sampleResponse;
    private InvestmentRequest sampleRequest;

    @BeforeEach
    void setUp() throws Exception {
        // Make the mocked JWT filter pass through so requests reach the controller
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        sampleRequest = InvestmentRequest.builder()
                .startupId(10L)
                .amount(new BigDecimal("50000.00"))
                .build();

        sampleResponse = InvestmentResponse.builder()
                .id(100L)
                .startupId(10L)
                .investorId(1L)
                .amount(new BigDecimal("50000.00"))
                .status(InvestmentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /investments
    // -------------------------------------------------------------------------

    @Test
    void createInvestment_withInvestorRole_shouldReturn201() throws Exception {
        // given
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_INVESTOR")));
        when(investmentCommandService.createInvestment(any(InvestmentRequest.class), eq(1L)))
                .thenReturn(sampleResponse);

        // when / then
        mockMvc.perform(post("/investments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createInvestment_withoutAuth_shouldReturn401() throws Exception {
        // when / then — no authentication token provided
        mockMvc.perform(post("/investments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createInvestment_withFounderRole_shouldReturn403() throws Exception {
        // given
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_FOUNDER")));

        // when / then — FOUNDER does not have INVESTOR role
        mockMvc.perform(post("/investments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /investments/startup/{startupId}
    // -------------------------------------------------------------------------

    @Test
    void getInvestmentsByStartup_shouldReturn200() throws Exception {
        // given
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_INVESTOR")));
        when(investmentQueryService.getInvestmentsByStartup(10L))
                .thenReturn(List.of(sampleResponse));

        // when / then
        mockMvc.perform(get("/investments/startup/10")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startupId").value(10));
    }

    // -------------------------------------------------------------------------
    // PUT /investments/{id}/approve
    // -------------------------------------------------------------------------

    @Test
    void approveInvestment_withFounderRole_shouldReturn200() throws Exception {
        // given
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_FOUNDER")));
        InvestmentResponse approvedResponse = InvestmentResponse.builder()
                .id(100L)
                .startupId(10L)
                .investorId(2L)
                .amount(new BigDecimal("50000.00"))
                .status(InvestmentStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .build();
        when(investmentCommandService.approveInvestment(eq(100L), eq(1L))).thenReturn(approvedResponse);

        // when / then
        mockMvc.perform(put("/investments/100/approve")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    // -------------------------------------------------------------------------
    // PUT /investments/{id}/reject
    // -------------------------------------------------------------------------

    @Test
    void rejectInvestment_withFounderRole_shouldReturn200() throws Exception {
        // given
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_FOUNDER")));
        InvestmentResponse rejectedResponse = InvestmentResponse.builder()
                .id(100L)
                .startupId(10L)
                .investorId(2L)
                .amount(new BigDecimal("50000.00"))
                .status(InvestmentStatus.REJECTED)
                .createdAt(LocalDateTime.now())
                .build();
        when(investmentCommandService.rejectInvestment(eq(100L), eq(1L))).thenReturn(rejectedResponse);

        // when / then
        mockMvc.perform(put("/investments/100/reject")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
