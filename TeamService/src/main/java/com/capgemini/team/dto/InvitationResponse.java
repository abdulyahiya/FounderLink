package com.capgemini.team.dto;

import com.capgemini.team.enums.InvitationStatus;
import com.capgemini.team.enums.TeamRole;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationResponse {

    private Long id;
    private Long startupId;
    private Long invitedUserId;
    private TeamRole role;
    private InvitationStatus status;
    private LocalDateTime createdAt;
}
