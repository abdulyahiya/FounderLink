package com.capgemini.team.event;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamInviteSentEvent implements Serializable {

    private Long invitationId;
    private Long startupId;
    private Long invitedUserId;
    private String role;
}
