package com.capgemini.team.service;

import com.capgemini.team.dto.InvitationRequest;
import com.capgemini.team.dto.InvitationResponse;
import com.capgemini.team.dto.RoleUpdateRequest;
import com.capgemini.team.dto.TeamMemberResponse;

public interface TeamCommandService {
    InvitationResponse inviteCoFounder(InvitationRequest request, Long founderId);
    TeamMemberResponse acceptInvitation(Long invitationId, Long userId);
    InvitationResponse rejectInvitation(Long invitationId, Long userId);
    TeamMemberResponse updateMemberRole(Long memberId, RoleUpdateRequest request, Long founderId);
}
