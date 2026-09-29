package com.capgemini.startup.service;

import com.capgemini.startup.dto.StartupResponse;
import com.capgemini.startup.enums.StartupStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface StartupQueryService {
    StartupResponse getStartupById(Long id);
    Page<StartupResponse> getAllApprovedStartups(Pageable pageable);
    Page<StartupResponse> getAllStartups(Pageable pageable);
    Page<StartupResponse> searchStartups(String industry, StartupStage stage,
                                         BigDecimal minFunding, BigDecimal maxFunding,
                                         String location, Pageable pageable);
    List<StartupResponse> getStartupsByFounderId(Long founderId);
}
