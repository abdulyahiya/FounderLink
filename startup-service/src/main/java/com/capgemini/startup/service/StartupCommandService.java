package com.capgemini.startup.service;

import com.capgemini.startup.dto.StartupRequest;
import com.capgemini.startup.dto.StartupResponse;

public interface StartupCommandService {
    StartupResponse createStartup(Long founderId, StartupRequest request);
    StartupResponse updateStartup(Long id, Long founderId, StartupRequest request);
    void deleteStartup(Long id, Long userId, boolean isAdmin);
    void followStartup(Long startupId, Long investorId);
    StartupResponse approveStartup(Long id);
    StartupResponse rejectStartup(Long id);
}
