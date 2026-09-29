package com.capgemini.investment.feign;


import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartupDTO {

    private Long id;
    private String name;
    private Long founderId;
    private String industry;
    private String stage;
    private BigDecimal fundingGoal;
}