package com.capgemini.payment.repository;

import com.capgemini.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRazorpayOrderId(String orderId);
    List<Payment> findByInvestorIdOrderByCreatedAtDesc(Long investorId);
    List<Payment> findByFounderIdOrderByCreatedAtDesc(Long founderId);
    List<Payment> findByStartupIdOrderByCreatedAtDesc(Long startupId);
}
