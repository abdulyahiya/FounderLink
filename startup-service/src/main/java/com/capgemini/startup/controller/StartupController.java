package com.capgemini.startup.controller;

import com.capgemini.startup.dto.StartupRequest;
import com.capgemini.startup.dto.StartupResponse;
import com.capgemini.startup.enums.StartupStage;
import com.capgemini.startup.service.StartupCommandService;
import com.capgemini.startup.service.StartupQueryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/startups")
public class
StartupController {

    private final StartupCommandService startupCommandService;
    private final StartupQueryService startupQueryService;

    public StartupController(StartupCommandService startupCommandService, StartupQueryService startupQueryService) {
        this.startupCommandService = startupCommandService;
        this.startupQueryService = startupQueryService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_FOUNDER')")
    public ResponseEntity<StartupResponse> createStartup(
            Authentication authentication,
            @Valid @RequestBody StartupRequest request) {

        Long founderId = Long.parseLong(authentication.getName());
        StartupResponse response = startupCommandService.createStartup(founderId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<StartupResponse>> getAllApprovedStartups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<StartupResponse> startups = startupQueryService.getAllApprovedStartups(pageable);
        return ResponseEntity.ok(startups);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StartupResponse> getStartupById(@PathVariable Long id) {
        StartupResponse response = startupQueryService.getStartupById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_FOUNDER') or hasAuthority('ROLE_COFOUNDER')")
    public ResponseEntity<StartupResponse> updateStartup(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody StartupRequest request) {

        Long founderId = Long.parseLong(authentication.getName());
        StartupResponse response = startupCommandService.updateStartup(id, founderId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_FOUNDER') or hasAuthority('ROLE_COFOUNDER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteStartup(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        boolean isAdmin = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        startupCommandService.deleteStartup(id, userId, isAdmin);
        return ResponseEntity.ok(Map.of("message", "Startup deleted successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<StartupResponse>> searchStartups(
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) StartupStage stage,
            @RequestParam(required = false) BigDecimal minFunding,
            @RequestParam(required = false) BigDecimal maxFunding,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<StartupResponse> results = startupQueryService.searchStartups(
                industry, stage, minFunding, maxFunding, location, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Page<StartupResponse>> getAllStartupsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<StartupResponse> startups = startupQueryService.getAllStartups(pageable);
        return ResponseEntity.ok(startups);
    }

    @PostMapping("/{id}/follow")
    @PreAuthorize("hasAuthority('ROLE_INVESTOR')")
    public ResponseEntity<Map<String, String>> followStartup(
            @PathVariable Long id,
            Authentication authentication) {

        Long investorId = Long.parseLong(authentication.getName());
        startupCommandService.followStartup(id, investorId);
        return new ResponseEntity<>(Map.of("message", "Startup followed successfully"), HttpStatus.OK);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<StartupResponse> approveStartup(@PathVariable Long id) {
        StartupResponse response = startupCommandService.approveStartup(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<StartupResponse> rejectStartup(@PathVariable Long id) {
        StartupResponse response = startupCommandService.rejectStartup(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/founder/{founderId}")
    public ResponseEntity<List<StartupResponse>> getStartupsByFounder(
            @PathVariable Long founderId) {

        List<StartupResponse> startups = startupQueryService.getStartupsByFounderId(founderId);
        return ResponseEntity.ok(startups);
    }
}
