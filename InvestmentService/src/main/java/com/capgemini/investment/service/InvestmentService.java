package com.capgemini.investment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capgemini.investment.dto.InvestmentRequest;
import com.capgemini.investment.dto.InvestmentResponse;
import com.capgemini.investment.entity.Investment;
import com.capgemini.investment.enums.InvestmentStatus;
import com.capgemini.investment.event.EventPublisher;
import com.capgemini.investment.event.InvestmentApprovedEvent;
import com.capgemini.investment.event.InvestmentCreatedEvent;
import com.capgemini.investment.exception.BadRequestException;
import com.capgemini.investment.exception.ResourceNotFoundException;
import com.capgemini.investment.exception.ServiceUnavailableException;
import com.capgemini.investment.exception.UnauthorizedException;
import com.capgemini.investment.feign.StartupClient;
import com.capgemini.investment.feign.StartupDTO;
import com.capgemini.investment.mapper.InvestmentMapper;
import com.capgemini.investment.repository.InvestmentRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentService implements InvestmentCommandService, InvestmentQueryService {

    private final InvestmentRepository investmentRepository;
    private final StartupClient startupClient;
    private final EventPublisher eventPublisher;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final InvestmentMapper investmentMapper;

    private StartupDTO fetchStartup(Long startupId) {
        return circuitBreakerFactory.create("startup-service").run(
                () -> startupClient.getStartupById(startupId),
                throwable -> {
                    log.error("[CIRCUIT BREAKER] startup-service unavailable: {}", throwable.getMessage());
                    throw new ServiceUnavailableException(
                            "Startup service is currently unavailable. Please try again later.");
                }
        );
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "investmentsByStartup", key = "#request.startupId"),
        @CacheEvict(value = "investmentsByInvestor", key = "#investorId")
    })
    public InvestmentResponse createInvestment(InvestmentRequest request, Long investorId) {
        StartupDTO startup = fetchStartup(request.getStartupId());
        if (startup == null) {
            throw new ResourceNotFoundException("Startup not found with id: " + request.getStartupId());
        }

        Investment investment = Investment.builder()
                .startupId(request.getStartupId())
                .investorId(investorId)
                .amount(request.getAmount())
                .status(InvestmentStatus.PENDING)
                .build();

        investment = investmentRepository.save(investment);

        eventPublisher.publishInvestmentCreated(InvestmentCreatedEvent.builder()
                .investmentId(investment.getId())
                .startupId(investment.getStartupId())
                .investorId(investment.getInvestorId())
                .founderId(startup.getFounderId())
                .amount(investment.getAmount())
                .build());

        log.info("Investment created: id={}, startupId={}, investorId={}",
                investment.getId(), investment.getStartupId(), investment.getInvestorId());

        return investmentMapper.toResponse(investment);
    }

    @Override
    @Cacheable(value = "investmentsByStartup", key = "#startupId")
    public List<InvestmentResponse> getInvestmentsByStartup(Long startupId) {
        return investmentRepository.findByStartupId(startupId).stream()
                .map(investmentMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Cacheable(value = "investmentsByInvestor", key = "#investorId")
    public List<InvestmentResponse> getInvestmentsByInvestor(Long investorId) {
        return investmentRepository.findByInvestorId(investorId).stream()
                .map(investmentMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "investmentsByStartup", allEntries = true),
        @CacheEvict(value = "investmentsByInvestor", allEntries = true)
    })
    public InvestmentResponse approveInvestment(Long investmentId, Long founderId) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found with id: " + investmentId));

        if (investment.getStatus() != InvestmentStatus.PENDING) {
            throw new BadRequestException("Investment is not in PENDING status");
        }

        StartupDTO startup = fetchStartup(investment.getStartupId());
        if (!startup.getFounderId().equals(founderId)) {
            throw new UnauthorizedException("Only the startup founder can approve investments");
        }

        investment.setStatus(InvestmentStatus.APPROVED);
        investment = investmentRepository.save(investment);

        eventPublisher.publishInvestmentApproved(InvestmentApprovedEvent.builder()
                .investmentId(investment.getId())
                .startupId(investment.getStartupId())
                .investorId(investment.getInvestorId())
                .amount(investment.getAmount())
                .build());

        log.info("Investment approved: id={}", investmentId);

        return investmentMapper.toResponse(investment);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "investmentsByStartup", allEntries = true),
        @CacheEvict(value = "investmentsByInvestor", allEntries = true)
    })
    public InvestmentResponse rejectInvestment(Long investmentId, Long founderId) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found with id: " + investmentId));

        if (investment.getStatus() != InvestmentStatus.PENDING) {
            throw new BadRequestException("Investment is not in PENDING status");
        }

        StartupDTO startup = fetchStartup(investment.getStartupId());
        if (!startup.getFounderId().equals(founderId)) {
            throw new UnauthorizedException("Only the startup founder can reject investments");
        }

        investment.setStatus(InvestmentStatus.REJECTED);
        investment = investmentRepository.save(investment);

        log.info("Investment rejected: id={}", investmentId);

        return investmentMapper.toResponse(investment);
    }

}
