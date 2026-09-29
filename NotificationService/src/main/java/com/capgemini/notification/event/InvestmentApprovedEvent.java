package com.capgemini.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentApprovedEvent {
    private Long investmentId;
    private Long startupId;
    private Long investorId;
    private BigDecimal amount;
}
