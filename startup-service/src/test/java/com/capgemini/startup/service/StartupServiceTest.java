package com.capgemini.startup.service;

import com.capgemini.startup.config.RabbitMQConfig;
import com.capgemini.startup.dto.StartupCreatedEvent;
import com.capgemini.startup.dto.StartupRequest;
import com.capgemini.startup.dto.StartupResponse;
import com.capgemini.startup.entity.Startup;
import com.capgemini.startup.entity.StartupFollower;
import com.capgemini.startup.enums.StartupStage;
import com.capgemini.startup.exception.DuplicateResourceException;
import com.capgemini.startup.exception.ResourceNotFoundException;
import com.capgemini.startup.exception.UnauthorizedAccessException;
import com.capgemini.startup.mapper.StartupMapper;
import com.capgemini.startup.repository.StartupFollowerRepository;
import com.capgemini.startup.repository.StartupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StartupServiceTest {

    @Mock
    private StartupRepository startupRepository;

    @Mock
    private StartupFollowerRepository startupFollowerRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private StartupMapper startupMapper;

    @InjectMocks
    private StartupServiceImpl startupService;

    private Startup startup;
    private StartupRequest startupRequest;

    @BeforeEach
    void setUp() {
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

        startup = Startup.builder()
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

        // Default mapper stub
        when(startupMapper.toResponse(any(Startup.class))).thenAnswer(inv -> {
            Startup s = inv.getArgument(0);
            return StartupResponse.builder()
                    .id(s.getId()).name(s.getName()).description(s.getDescription())
                    .industry(s.getIndustry()).problemStatement(s.getProblemStatement())
                    .solution(s.getSolution()).fundingGoal(s.getFundingGoal())
                    .stage(s.getStage()).location(s.getLocation())
                    .founderId(s.getFounderId()).isApproved(s.getIsApproved())
                    .isRejected(s.getIsRejected()).createdAt(s.getCreatedAt())
                    .updatedAt(s.getUpdatedAt()).build();
        });
    }

    // -----------------------------------------------------------------------
    // createStartup
    // -----------------------------------------------------------------------

    @Test
    void createStartup_shouldSaveAndPublishEvent() {
        // given
        when(startupRepository.save(any(Startup.class))).thenReturn(startup);

        // when
        StartupResponse response = startupService.createStartup(1L, startupRequest);

        // then
        verify(startupRepository).save(any(Startup.class));
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.STARTUP_CREATED_ROUTING_KEY),
                any(StartupCreatedEvent.class)
        );
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("GreenTech");
        assertThat(response.getFounderId()).isEqualTo(1L);
        assertThat(response.getIsApproved()).isFalse();
    }

    // -----------------------------------------------------------------------
    // getStartupById
    // -----------------------------------------------------------------------

    @Test
    void getStartupById_whenNotFound_shouldThrowResourceNotFoundException() {
        // given
        when(startupRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> startupService.getStartupById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getStartupById_whenFound_shouldReturnStartupResponse() {
        // given
        when(startupRepository.findById(1L)).thenReturn(Optional.of(startup));

        // when
        StartupResponse response = startupService.getStartupById(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("GreenTech");
    }

    // -----------------------------------------------------------------------
    // updateStartup
    // -----------------------------------------------------------------------

    @Test
    void updateStartup_whenNotFound_shouldThrowResourceNotFoundException() {
        // given
        when(startupRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> startupService.updateStartup(99L, 1L, startupRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateStartup_whenNotFounder_shouldThrowUnauthorizedAccessException() {
        // given — startup belongs to founder 1, but caller is 2
        when(startupRepository.findById(1L)).thenReturn(Optional.of(startup));

        // when / then
        assertThatThrownBy(() -> startupService.updateStartup(1L, 2L, startupRequest))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void updateStartup_whenValid_shouldReturnUpdatedResponse() {
        // given
        StartupRequest updatedRequest = StartupRequest.builder()
                .name("GreenTech V2")
                .description("Updated description")
                .industry("CleanTech")
                .problemStatement("More emissions")
                .solution("Better capture")
                .fundingGoal(new BigDecimal("750000.00"))
                .stage(StartupStage.MVP)
                .location("Amsterdam")
                .build();

        Startup updatedStartup = Startup.builder()
                .id(1L)
                .name("GreenTech V2")
                .description("Updated description")
                .industry("CleanTech")
                .problemStatement("More emissions")
                .solution("Better capture")
                .fundingGoal(new BigDecimal("750000.00"))
                .stage(StartupStage.MVP)
                .location("Amsterdam")
                .founderId(1L)
                .isApproved(false)
                .build();

        when(startupRepository.findById(1L)).thenReturn(Optional.of(startup));
        when(startupRepository.save(any(Startup.class))).thenReturn(updatedStartup);

        // when
        StartupResponse response = startupService.updateStartup(1L, 1L, updatedRequest);

        // then
        assertThat(response.getName()).isEqualTo("GreenTech V2");
        assertThat(response.getStage()).isEqualTo(StartupStage.MVP);
        verify(startupRepository).save(startup);
    }

    // -----------------------------------------------------------------------
    // deleteStartup
    // -----------------------------------------------------------------------

    @Test
    void deleteStartup_whenNotFounderAndNotAdmin_shouldThrowUnauthorizedAccessException() {
        // given — startup belongs to founder 1, caller is user 2, not admin
        when(startupRepository.findById(1L)).thenReturn(Optional.of(startup));

        // when / then
        assertThatThrownBy(() -> startupService.deleteStartup(1L, 2L, false))
                .isInstanceOf(UnauthorizedAccessException.class);

        verify(startupRepository, never()).delete(any(Startup.class));
    }

    @Test
    void deleteStartup_whenAdmin_shouldDelete() {
        // given — caller is an admin (not the founder), isAdmin = true
        when(startupRepository.findById(1L)).thenReturn(Optional.of(startup));

        // when
        startupService.deleteStartup(1L, 99L, true);

        // then
        verify(startupRepository).delete(startup);
    }

    @Test
    void deleteStartup_whenFounder_shouldDelete() {
        // given — caller is the founder, not an admin
        when(startupRepository.findById(1L)).thenReturn(Optional.of(startup));

        // when
        startupService.deleteStartup(1L, 1L, false);

        // then
        verify(startupRepository).delete(startup);
    }

    // -----------------------------------------------------------------------
    // followStartup
    // -----------------------------------------------------------------------

    @Test
    void followStartup_whenNotFound_shouldThrowResourceNotFoundException() {
        // given
        when(startupRepository.existsById(99L)).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> startupService.followStartup(99L, 2L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void followStartup_whenAlreadyFollowing_shouldThrowDuplicateResourceException() {
        // given
        when(startupRepository.existsById(1L)).thenReturn(true);
        when(startupFollowerRepository.existsByStartupIdAndInvestorId(1L, 2L)).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> startupService.followStartup(1L, 2L))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void followStartup_whenValid_shouldSaveFollower() {
        // given
        when(startupRepository.existsById(1L)).thenReturn(true);
        when(startupFollowerRepository.existsByStartupIdAndInvestorId(1L, 2L)).thenReturn(false);

        // when
        startupService.followStartup(1L, 2L);

        // then
        ArgumentCaptor<StartupFollower> captor = ArgumentCaptor.forClass(StartupFollower.class);
        verify(startupFollowerRepository).save(captor.capture());
        assertThat(captor.getValue().getStartupId()).isEqualTo(1L);
        assertThat(captor.getValue().getInvestorId()).isEqualTo(2L);
    }

    // -----------------------------------------------------------------------
    // approveStartup
    // -----------------------------------------------------------------------

    @Test
    void approveStartup_whenValid_shouldSetApprovedTrue() {
        // given
        startup.setIsApproved(false);
        Startup approvedStartup = Startup.builder()
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
                .isApproved(true)
                .build();

        when(startupRepository.findById(1L)).thenReturn(Optional.of(startup));
        when(startupRepository.save(startup)).thenReturn(approvedStartup);

        // when
        StartupResponse response = startupService.approveStartup(1L);

        // then
        assertThat(response.getIsApproved()).isTrue();
        verify(startupRepository).save(startup);
    }

    // -----------------------------------------------------------------------
    // getAllApprovedStartups
    // -----------------------------------------------------------------------

    @Test
    void getAllApprovedStartups_shouldReturnPageOfResponses() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        startup.setIsApproved(true);
        Page<Startup> page = new PageImpl<>(List.of(startup));
        when(startupRepository.findByIsApprovedTrue(pageable)).thenReturn(page);

        // when
        Page<StartupResponse> result = startupService.getAllApprovedStartups(pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsApproved()).isTrue();
    }

    // -----------------------------------------------------------------------
    // getStartupsByFounderId
    // -----------------------------------------------------------------------

    @Test
    void getStartupsByFounderId_shouldReturnListOfResponses() {
        // given
        when(startupRepository.findByFounderId(1L)).thenReturn(List.of(startup));

        // when
        List<StartupResponse> responses = startupService.getStartupsByFounderId(1L);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getFounderId()).isEqualTo(1L);
    }
}
