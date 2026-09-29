package com.capgemini.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.capgemini.team.entity.TeamMember;
import com.capgemini.team.enums.TeamRole;

import java.util.List;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByStartupId(Long startupId);

    boolean existsByStartupIdAndUserId(Long startupId, Long userId);

    boolean existsByStartupIdAndRole(Long startupId, TeamRole role);
}
