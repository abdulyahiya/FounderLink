package com.capgemini.investment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.capgemini.investment.entity.Investment;

import java.util.List;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {

    List<Investment> findByStartupId(Long startupId);

    List<Investment> findByInvestorId(Long investorId);
}
