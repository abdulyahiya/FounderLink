package com.capgemini.investment.service;

import com.capgemini.investment.dto.InvestmentRequest;
import com.capgemini.investment.dto.InvestmentResponse;
import com.capgemini.investment.entity.Investment;
import com.capgemini.investment.enums.InvestmentStatus;
import com.capgemini.investment.event.EventPublisher;
import com.capgemini.investment.exception.BadRequestException;
import com.capgemini.investment.exception.ServiceUnavailableException;
import com.capgemini.investment.exception.UnauthorizedException;
import com.capgemini.investment.feign.StartupClient;
import com.capgemini.investment.feign.StartupDTO;
import com.capgemini.investment.mapper.InvestmentMapper;
import com.capgemini.investment.repository.InvestmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvestmentServiceTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private StartupClient startupClient;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private org.springframework.cloud.client.circuitbreaker.CircuitBreaker circuitBreaker;

    @Mock
    private InvestmentMapper investmentMapper;

    @InjectMocks
    private InvestmentService investmentService;

    private StartupDTO sampleStartup;
    private Investment sampleInvestment;
    private InvestmentRequest sampleRequest;

    @BeforeEach
    void setUp() {
        when(circuitBreakerFactory.create("startup-service")).thenReturn(circuitBreaker);
        when(circuitBreaker.run(any(), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        sampleStartup = StartupDTO.builder()
                .id(10L)
                .name("TechStartup")
                .founderId(1L)
                .build();

        sampleInvestment = Investment.builder()
                .id(100L)
                .startupId(10L)
                .investorId(2L)
                .amount(new BigDecimal("50000.00"))
                .status(InvestmentStatus.PENDING)
                .build();
        // Simulate @PrePersist
        sampleInvestment.setCreatedAt(LocalDateTime.now());

        sampleRequest = InvestmentRequest.builder()
                .startupId(10L)
                .amount(new BigDecimal("50000.00"))
                .build();

        // Default mapper stub — returns a response matching the investment's fields
        when(investmentMapper.toResponse(any(Investment.class))).thenAnswer(inv -> {
            Investment i = inv.getArgument(0);
            return InvestmentResponse.builder()
                    .id(i.getId()).startupId(i.getStartupId()).investorId(i.getInvestorId())
                    .amount(i.getAmount()).status(i.getStatus()).createdAt(i.getCreatedAt())
                    .build();
        });
    }

    // -------------------------------------------------------------------------
    // createInvestment
    // -------------------------------------------------------------------------

    @Test
    void createInvestment_whenStartupAvailable_shouldSaveAndReturnResponse() {
        // given
        when(startupClient.getStartupById(10L)).thenReturn(sampleStartup);
        when(investmentRepository.save(any(Investment.class))).thenReturn(sampleInvestment);
        doNothing().when(eventPublisher).publishInvestmentCreated(any());

        // when
        InvestmentResponse response = investmentService.createInvestment(sampleRequest, 2L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStartupId()).isEqualTo(10L);
        assertThat(response.getInvestorId()).isEqualTo(2L);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(response.getStatus()).isEqualTo(InvestmentStatus.PENDING);
        verify(investmentRepository).save(any(Investment.class));
        verify(eventPublisher).publishInvestmentCreated(any());
    }

    @Test
    void createInvestment_whenStartupServiceDown_shouldThrowServiceUnavailableException() {
        // given — use doAnswer().when() to avoid triggering the setUp stub during recording
        doAnswer(invocation -> {
            Function<Throwable, ?> fallback = invocation.getArgument(1);
            return fallback.apply(new RuntimeException("Service down"));
        }).when(circuitBreaker).run(any(), any());

        // when / then
        assertThatThrownBy(() -> investmentService.createInvestment(sampleRequest, 2L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Startup service is currently unavailable");

        verify(investmentRepository, never()).save(any());
        verify(eventPublisher, never()).publishInvestmentCreated(any());
    }

    // -------------------------------------------------------------------------
    // getInvestmentsByStartup
    // -------------------------------------------------------------------------

    @Test
    void getInvestmentsByStartup_shouldReturnList() {
        // given
        Investment second = Investment.builder()
                .id(101L)
                .startupId(10L)
                .investorId(3L)
                .amount(new BigDecimal("20000.00"))
                .status(InvestmentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        when(investmentRepository.findByStartupId(10L)).thenReturn(List.of(sampleInvestment, second));

        // when
        List<InvestmentResponse> result = investmentService.getInvestmentsByStartup(10L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(InvestmentResponse::getStartupId).containsOnly(10L);
        verify(investmentRepository).findByStartupId(10L);
    }

    // -------------------------------------------------------------------------
    // getInvestmentsByInvestor
    // -------------------------------------------------------------------------

    @Test
    void getInvestmentsByInvestor_shouldReturnList() {
        // given
        when(investmentRepository.findByInvestorId(2L)).thenReturn(List.of(sampleInvestment));

        // when
        List<InvestmentResponse> result = investmentService.getInvestmentsByInvestor(2L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvestorId()).isEqualTo(2L);
        verify(investmentRepository).findByInvestorId(2L);
    }

    // -------------------------------------------------------------------------
    // approveInvestment
    // -------------------------------------------------------------------------

    @Test
    void approveInvestment_whenNotPending_shouldThrowBadRequestException() {
        // given
        Investment alreadyApproved = Investment.builder()
                .id(100L)
                .startupId(10L)
                .investorId(2L)
                .amount(new BigDecimal("50000.00"))
                .status(InvestmentStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .build();
        when(investmentRepository.findById(100L)).thenReturn(Optional.of(alreadyApproved));

        // when / then
        assertThatThrownBy(() -> investmentService.approveInvestment(100L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PENDING");

        verify(investmentRepository, never()).save(any());
    }

    @Test
    void approveInvestment_whenNotFounder_shouldThrowUnauthorizedException() {
        // given
        when(investmentRepository.findById(100L)).thenReturn(Optional.of(sampleInvestment));
        when(startupClient.getStartupById(10L)).thenReturn(sampleStartup); // founderId = 1L

        // when / then — pass a different founderId
        assertThatThrownBy(() -> investmentService.approveInvestment(100L, 99L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("founder");

        verify(investmentRepository, never()).save(any());
    }

    @Test
    void approveInvestment_whenValid_shouldSetApprovedStatus() {
        // given
        when(investmentRepository.findById(100L)).thenReturn(Optional.of(sampleInvestment));
        when(startupClient.getStartupById(10L)).thenReturn(sampleStartup); // founderId = 1L
        Investment savedInvestment = Investment.builder()
                .id(100L)
                .startupId(10L)
                .investorId(2L)
                .amount(new BigDecimal("50000.00"))
                .status(InvestmentStatus.APPROVED)
                .createdAt(sampleInvestment.getCreatedAt())
                .build();
        when(investmentRepository.save(any(Investment.class))).thenReturn(savedInvestment);
        doNothing().when(eventPublisher).publishInvestmentApproved(any());

        // when
        InvestmentResponse response = investmentService.approveInvestment(100L, 1L);

        // then
        assertThat(response.getStatus()).isEqualTo(InvestmentStatus.APPROVED);
        verify(investmentRepository).save(any(Investment.class));
        verify(eventPublisher).publishInvestmentApproved(any());
    }

    // -------------------------------------------------------------------------
    // rejectInvestment
    // -------------------------------------------------------------------------

    @Test
    void rejectInvestment_whenValid_shouldSetRejectedStatus() {
        // given
        when(investmentRepository.findById(100L)).thenReturn(Optional.of(sampleInvestment));
        when(startupClient.getStartupById(10L)).thenReturn(sampleStartup); // founderId = 1L
        Investment savedInvestment = Investment.builder()
                .id(100L)
                .startupId(10L)
                .investorId(2L)
                .amount(new BigDecimal("50000.00"))
                .status(InvestmentStatus.REJECTED)
                .createdAt(sampleInvestment.getCreatedAt())
                .build();
        when(investmentRepository.save(any(Investment.class))).thenReturn(savedInvestment);

        // when
        InvestmentResponse response = investmentService.rejectInvestment(100L, 1L);

        // then
        assertThat(response.getStatus()).isEqualTo(InvestmentStatus.REJECTED);
        verify(investmentRepository).save(any(Investment.class));
    }
}
