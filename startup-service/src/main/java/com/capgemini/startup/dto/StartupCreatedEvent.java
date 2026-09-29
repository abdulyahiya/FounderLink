package com.capgemini.startup.dto;


import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartupCreatedEvent implements Serializable {

    private Long startupId;
    private Long founderId;
    private String startupName;
    private String industry;
    private BigDecimal fundingGoal;
}
