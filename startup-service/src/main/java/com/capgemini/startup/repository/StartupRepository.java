package com.capgemini.startup.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.capgemini.startup.entity.Startup;

import java.util.List;

@Repository
public interface StartupRepository extends JpaRepository<Startup, Long>, JpaSpecificationExecutor<Startup> {

    List<Startup> findByFounderId(Long founderId);

    Page<Startup> findByIsApprovedTrue(Pageable pageable);
}