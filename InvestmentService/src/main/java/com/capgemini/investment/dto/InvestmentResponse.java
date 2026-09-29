package com.capgemini.investment.dto;

import com.capgemini.investment.enums.InvestmentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentResponse {

    private Long id;
    private Long startupId;
    private Long investorId;
    private BigDecimal amount;
    private InvestmentStatus status;
    private LocalDateTime createdAt;
}
