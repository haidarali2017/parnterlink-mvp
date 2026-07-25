package com.example.partnerlink.infrastructure.mun;

import com.example.partnerlink.domain.ApplicationStatus;

import java.util.concurrent.CompletableFuture;

/**
 * External ACQ/MUN screening client. Production would call real MUN HTTP APIs;
 * this take-home uses a mock implementation.
 */
public interface MunClient {

    /**
     * Start async screening for an application. Completes with APPROVED or REJECTED.
     * Callers must apply their own timeout; this future itself does not encode timeout.
     */
    CompletableFuture<ApplicationStatus> screenAsync(String applicationId);
}
