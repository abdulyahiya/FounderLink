package com.capgemini.startup.mapper;

import com.capgemini.startup.dto.StartupResponse;
import com.capgemini.startup.entity.Startup;
import org.springframework.stereotype.Component;

@Component
public class StartupMapper {

    public StartupResponse toResponse(Startup startup) {
        return StartupResponse.builder()
                .id(startup.getId())
                .name(startup.getName())
                .description(startup.getDescription())
                .industry(startup.getIndustry())
                .problemStatement(startup.getProblemStatement())
                .solution(startup.getSolution())
                .fundingGoal(startup.getFundingGoal())
                .stage(startup.getStage())
                .location(startup.getLocation())
                .founderId(startup.getFounderId())
                .isApproved(startup.getIsApproved())
                .isRejected(startup.getIsRejected())
                .createdAt(startup.getCreatedAt())
                .updatedAt(startup.getUpdatedAt())
                .build();
    }
}
