package com.capgemini.payment.service;

import com.capgemini.payment.dto.CreateOrderRequest;
import com.capgemini.payment.dto.VerifyPaymentRequest;
import com.capgemini.payment.entity.Payment;
import com.razorpay.RazorpayException;

import java.util.List;
import java.util.Map;

public interface IPaymentService {
    Map<String, Object> createOrder(CreateOrderRequest request) throws RazorpayException;
    Map<String, Object> verifyPayment(VerifyPaymentRequest request);
    Map<String, Object> acceptPayment(Long paymentId);
    Map<String, Object> rejectPayment(Long paymentId);
    List<Payment> getPaymentsByInvestor(Long investorId);
    List<Payment> getPaymentsByFounder(Long founderId);
    List<Payment> getPaymentsByStartup(Long startupId);
}
