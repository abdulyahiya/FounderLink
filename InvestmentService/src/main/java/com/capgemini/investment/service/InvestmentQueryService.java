package com.capgemini.investment.service;

import com.capgemini.investment.dto.InvestmentResponse;

import java.util.List;

public interface InvestmentQueryService {
    List<InvestmentResponse> getInvestmentsByStartup(Long startupId);
    List<InvestmentResponse> getInvestmentsByInvestor(Long investorId);
}
