package com.capgemini.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamInviteSentEvent {
    private Long startupId;
    private Long invitedUserId;
    private String role;
}
