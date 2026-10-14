package com.capgemini.payment.service;

import com.capgemini.payment.dto.CreateOrderRequest;
import com.capgemini.payment.dto.VerifyPaymentRequest;
import com.capgemini.payment.entity.Payment;
import com.capgemini.payment.saga.PaymentSaga;
import com.razorpay.RazorpayException;

import java.util.List;
import java.util.Map;

public interface IPaymentService {
    Map<String, Object> createOrder(CreateOrderRequest request) throws RazorpayException;
    Map<String, Object> createOrder(CreateOrderRequest request, Long callerUserId, boolean isAdmin) throws RazorpayException;
    Map<String, Object> verifyPayment(VerifyPaymentRequest request);
    Map<String, Object> acceptPayment(Long paymentId);
    Map<String, Object> acceptPayment(Long paymentId, Long callerUserId, boolean isAdmin);
    Map<String, Object> rejectPayment(Long paymentId);
    Map<String, Object> rejectPayment(Long paymentId, Long callerUserId, boolean isAdmin);
    List<Payment> getPaymentsByInvestor(Long investorId);
    List<Payment> getPaymentsByInvestor(Long investorId, Long callerUserId, boolean isAdmin);
    List<Payment> getPaymentsByFounder(Long founderId);
    List<Payment> getPaymentsByFounder(Long founderId, Long callerUserId, boolean isAdmin);
    List<Payment> getPaymentsByStartup(Long startupId);
    List<Payment> getPaymentsByStartup(Long startupId, Long callerUserId, boolean isAdmin);
    PaymentSaga getSagaStatus(Long paymentId, Long callerUserId, boolean isAdmin);
}
