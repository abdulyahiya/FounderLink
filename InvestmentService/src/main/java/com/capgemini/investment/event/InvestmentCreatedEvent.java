package com.capgemini.investment.event;


import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentCreatedEvent implements Serializable {

    private Long investmentId;
    private Long startupId;
    private Long investorId;
    private Long founderId;
    private BigDecimal amount;
}
