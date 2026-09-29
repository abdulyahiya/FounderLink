package com.capgemini.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentCreatedEvent {
    private Long investmentId;
    private Long startupId;
    private Long investorId;
    private Long founderId;
    private BigDecimal amount;
}
