package com.example.partnerlink.application;

import com.example.partnerlink.domain.ApplicationStatus;
import com.example.partnerlink.domain.IllegalStatusTransitionException;
import com.example.partnerlink.domain.MerchantApplication;
import com.example.partnerlink.infrastructure.mun.MunClient;
import com.example.partnerlink.infrastructure.mybatis.MerchantApplicationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Use-case boundary for merchant apply.
 *
 * <p>{@code @Transactional} lives on this service (not the controller / mapper).
 * External MUN I/O runs AFTER commit via {@code afterCommit}, so retries never
 * double-issue merchant numbers (UNIQUE {@code application_id} + assign-if-null).
 */
@Service
public class MerchantApplicationService {

    private static final Logger log = LoggerFactory.getLogger(MerchantApplicationService.class);

    private final MerchantApplicationMapper mapper;
    private final MunClient munClient;
    private final ScreeningOutcomeHandler screeningOutcomeHandler;
    private final long munTimeoutMs;

    public MerchantApplicationService(
            MerchantApplicationMapper mapper,
            MunClient munClient,
            ScreeningOutcomeHandler screeningOutcomeHandler,
            @Value("${partnerlink.mun.timeout-ms:2000}") long munTimeoutMs) {
        this.mapper = mapper;
        this.munClient = munClient;
        this.screeningOutcomeHandler = screeningOutcomeHandler;
        this.munTimeoutMs = munTimeoutMs;
    }

    /**
     * Idempotent apply: same applicationId always returns the existing row;
     * MUN is invoked only on the first successful insert.
     */
    @Transactional
    public ApplyResult apply(String applicationId, String merchantName) {
        MerchantApplication existing = mapper.findByApplicationId(applicationId);
        if (existing != null) {
            log.info("Idempotent hit applicationId={} status={}", applicationId, existing.getStatus());
            return new ApplyResult(existing, false);
        }

        MerchantApplication created = new MerchantApplication();
        created.setApplicationId(applicationId);
        created.setMerchantName(merchantName);
        created.setStatus(ApplicationStatus.APPLIED);

        try {
            mapper.insert(created);
        } catch (DuplicateKeyException ex) {
            MerchantApplication raced = mapper.findByApplicationId(applicationId);
            if (raced != null) {
                return new ApplyResult(raced, false);
            }
            throw ex;
        }

        transition(applicationId, ApplicationStatus.APPLIED, ApplicationStatus.SCREENING, null);

        String merchantNumber = "M-" + applicationId.replace("-", "").substring(0, 12).toUpperCase();
        mapper.assignMerchantNumber(applicationId, merchantNumber);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                startScreening(applicationId);
            }
        });

        return new ApplyResult(mapper.findByApplicationId(applicationId), true);
    }

    public record ApplyResult(MerchantApplication application, boolean created) {
    }

    @Transactional(readOnly = true)
    public MerchantApplication getByApplicationId(String applicationId) {
        MerchantApplication app = mapper.findByApplicationId(applicationId);
        if (app == null) {
            throw new ApplicationNotFoundException(applicationId);
        }
        return app;
    }

    /**
     * Explicit transition API for tests / ops — rejects illegal edges.
     */
    @Transactional
    public MerchantApplication transitionStatus(String applicationId, ApplicationStatus toStatus) {
        MerchantApplication app = getByApplicationId(applicationId);
        transition(applicationId, app.getStatus(), toStatus, null);
        return mapper.findByApplicationId(applicationId);
    }

    private void startScreening(String applicationId) {
        CompletableFuture<ApplicationStatus> future = munClient.screenAsync(applicationId);
        future.orTimeout(munTimeoutMs, TimeUnit.MILLISECONDS)
                .whenComplete((result, error) -> screeningOutcomeHandler.handle(applicationId, result, error));
    }

    private void transition(String applicationId, ApplicationStatus from, ApplicationStatus to, String failureReason) {
        from.assertCanTransitionTo(to);
        int updated = mapper.updateStatus(applicationId, from, to, failureReason);
        if (updated != 1) {
            MerchantApplication latest = mapper.findByApplicationId(applicationId);
            ApplicationStatus actual = latest != null ? latest.getStatus() : from;
            throw new IllegalStatusTransitionException(actual, to);
        }
    }

    public static String newApplicationId() {
        return UUID.randomUUID().toString();
    }
}
