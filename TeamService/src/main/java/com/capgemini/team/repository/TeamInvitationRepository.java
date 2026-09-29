package com.capgemini.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.capgemini.team.entity.TeamInvitation;
import com.capgemini.team.enums.InvitationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {

    List<TeamInvitation> findByInvitedUserIdAndStatus(Long invitedUserId, InvitationStatus status);

    List<TeamInvitation> findByInvitedUserIdOrderByCreatedAtDesc(Long invitedUserId);

    Optional<TeamInvitation> findByStartupIdAndInvitedUserIdAndStatus(Long startupId, Long invitedUserId, InvitationStatus status);
}
