package com.capgemini.payment.saga;

public enum SagaStatus {
    IN_PROGRESS,     // saga is running normally
    COMPLETED,       // all steps succeeded
    COMPENSATING,    // a failure occurred — compensating transactions are running
    COMPENSATED,     // compensation completed — money returned to investor
    FAILED           // compensation itself failed — requires manual intervention
}