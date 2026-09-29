package com.capgemini.team.service;

import com.capgemini.team.dto.InvitationResponse;
import com.capgemini.team.dto.TeamMemberResponse;

import java.util.List;

public interface TeamQueryService {
    List<TeamMemberResponse> getTeamByStartup(Long startupId);
    List<InvitationResponse> getMyInvitations(Long userId);
}
