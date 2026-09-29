package com.capgemini.startup.dto;

import com.capgemini.startup.enums.StartupStage;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartupResponse {

    private Long id;
    private String name;
    private String description;
    private String industry;
    private String problemStatement;
    private String solution;
    private BigDecimal fundingGoal;
    private StartupStage stage;
    private String location;
    private Long founderId;
    private Boolean isApproved;
    private Boolean isRejected;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
