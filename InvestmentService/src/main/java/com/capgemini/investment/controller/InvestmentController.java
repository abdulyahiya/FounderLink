package com.capgemini.investment.controller;


import com.capgemini.investment.dto.InvestmentRequest;
import com.capgemini.investment.dto.InvestmentResponse;
import com.capgemini.investment.service.InvestmentCommandService;
import com.capgemini.investment.service.InvestmentQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/investments")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentCommandService investmentCommandService;
    private final InvestmentQueryService investmentQueryService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_INVESTOR')")
    public ResponseEntity<InvestmentResponse> createInvestment(
            @Valid @RequestBody InvestmentRequest request,
            Authentication authentication) {
        Long investorId = Long.parseLong(authentication.getName());
        InvestmentResponse response = investmentCommandService.createInvestment(request, investorId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/startup/{startupId}")
    public ResponseEntity<List<InvestmentResponse>> getInvestmentsByStartup(
            @PathVariable Long startupId) {
        return ResponseEntity.ok(investmentQueryService.getInvestmentsByStartup(startupId));
    }

    @GetMapping("/investor/{investorId}")
    public ResponseEntity<List<InvestmentResponse>> getInvestmentsByInvestor(
            @PathVariable Long investorId) {
        return ResponseEntity.ok(investmentQueryService.getInvestmentsByInvestor(investorId));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_FOUNDER') or hasAuthority('ROLE_COFOUNDER')")
    public ResponseEntity<InvestmentResponse> approveInvestment(
            @PathVariable Long id,
            Authentication authentication) {
        Long founderId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(investmentCommandService.approveInvestment(id, founderId));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_FOUNDER') or hasAuthority('ROLE_COFOUNDER')")
    public ResponseEntity<InvestmentResponse> rejectInvestment(
            @PathVariable Long id,
            Authentication authentication) {
        Long founderId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(investmentCommandService.rejectInvestment(id, founderId));
    }
}

