package com.capgemini.payment.saga;

public enum SagaStep {
    ORDER_CREATED,        // Razorpay order created, payment not yet captured
    PAYMENT_CAPTURED,     // Investor's payment captured by Razorpay
    AWAITING_APPROVAL,    // Founder notified, waiting for accept/reject
    ACCEPTED,             // Founder accepted — investment confirmed
    COMPENSATING,         // Founder rejected — initiating Razorpay refund
    COMPENSATED,          // Refund issued — money returned to investor
    FAILED                // Compensation itself failed — manual action needed
}
