package com.capgemini.team.dto;

import com.capgemini.team.enums.TeamRole;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMemberResponse {

    private Long id;
    private Long startupId;
    private Long userId;
    private TeamRole role;
    private LocalDateTime joinedAt;
}
