package com.capgemini.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartupCreatedEvent {
    private Long startupId;
    private Long founderId;
    private String industry;
    private BigDecimal fundingGoal;
}
