package com.capgemini.startup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.capgemini.startup.entity.StartupFollower;

@Repository
public interface StartupFollowerRepository extends JpaRepository<StartupFollower, Long> {

    boolean existsByStartupIdAndInvestorId(Long startupId, Long investorId);
}