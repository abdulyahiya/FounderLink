package com.capgemini.investment.service;

import com.capgemini.investment.dto.InvestmentRequest;
import com.capgemini.investment.dto.InvestmentResponse;

public interface InvestmentCommandService {
    InvestmentResponse createInvestment(InvestmentRequest request, Long investorId);
    InvestmentResponse approveInvestment(Long investmentId, Long founderId);
    InvestmentResponse rejectInvestment(Long investmentId, Long founderId);
}
