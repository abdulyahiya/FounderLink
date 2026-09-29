package com.capgemini.payment.saga;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentSagaRepository extends JpaRepository<PaymentSaga, Long> {
    Optional<PaymentSaga> findByPaymentId(Long paymentId);
    Optional<PaymentSaga> findByRazorpayOrderId(String razorpayOrderId);
}