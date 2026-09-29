package com.capgemini.startup.dto;

import com.capgemini.startup.enums.StartupStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartupRequest {

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String industry;

    private String problemStatement;

    private String solution;

    @NotNull
    private BigDecimal fundingGoal;

    @NotNull
    private StartupStage stage;

    private String location;
}
