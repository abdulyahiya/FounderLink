package com.capgemini.team.controller;

import com.capgemini.team.dto.InvitationRequest;
import com.capgemini.team.dto.InvitationResponse;
import com.capgemini.team.dto.RoleUpdateRequest;
import com.capgemini.team.dto.TeamMemberResponse;
import com.capgemini.team.service.TeamCommandService;
import com.capgemini.team.service.TeamQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamCommandService teamCommandService;
    private final TeamQueryService teamQueryService;

    // Founder invites someone to the startup team
    @PostMapping("/invite")
    @PreAuthorize("hasAuthority('ROLE_FOUNDER') or hasAuthority('ROLE_COFOUNDER')")
    public ResponseEntity<InvitationResponse> inviteCoFounder(
            @Valid @RequestBody InvitationRequest request,
            Authentication authentication) {
        Long founderId = Long.parseLong(authentication.getName());
        InvitationResponse response = teamCommandService.inviteCoFounder(request, founderId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Invited user accepts the invitation (any authenticated user — service validates ownership)
    @PostMapping("/join/{invitationId}")
    public ResponseEntity<TeamMemberResponse> acceptInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return new ResponseEntity<>(teamCommandService.acceptInvitation(invitationId, userId), HttpStatus.OK);
    }

    // Invited user rejects the invitation (any authenticated user — service validates ownership)
    @PutMapping("/reject/{invitationId}")
    public ResponseEntity<InvitationResponse> rejectInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(teamCommandService.rejectInvitation(invitationId, userId));
    }

    // Founder updates a team member's role
    @PutMapping("/{memberId}/role")
    @PreAuthorize("hasAuthority('ROLE_FOUNDER') or hasAuthority('ROLE_COFOUNDER')")
    public ResponseEntity<TeamMemberResponse> updateMemberRole(
            @PathVariable Long memberId,
            @Valid @RequestBody RoleUpdateRequest request,
            Authentication authentication) {
        Long founderId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(teamCommandService.updateMemberRole(memberId, request, founderId));
    }

    // Get all team members of a startup
    @GetMapping("/startup/{startupId}")
    public ResponseEntity<List<TeamMemberResponse>> getTeamByStartup(
            @PathVariable Long startupId) {
        return ResponseEntity.ok(teamQueryService.getTeamByStartup(startupId));
    }

    // Get pending invitations for the currently authenticated user
    @GetMapping("/invitations/my")
    public ResponseEntity<List<InvitationResponse>> getMyInvitations(
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(teamQueryService.getMyInvitations(userId));
    }
}
