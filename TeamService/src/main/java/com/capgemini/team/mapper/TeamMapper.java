package com.capgemini.team.mapper;

import com.capgemini.team.dto.InvitationResponse;
import com.capgemini.team.dto.TeamMemberResponse;
import com.capgemini.team.entity.TeamInvitation;
import com.capgemini.team.entity.TeamMember;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

    public InvitationResponse toInvitationResponse(TeamInvitation invitation) {
        return InvitationResponse.builder()
                .id(invitation.getId())
                .startupId(invitation.getStartupId())
                .invitedUserId(invitation.getInvitedUserId())
                .role(invitation.getRole())
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .build();
    }

    public TeamMemberResponse toMemberResponse(TeamMember member) {
        return TeamMemberResponse.builder()
                .id(member.getId())
                .startupId(member.getStartupId())
                .userId(member.getUserId())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
