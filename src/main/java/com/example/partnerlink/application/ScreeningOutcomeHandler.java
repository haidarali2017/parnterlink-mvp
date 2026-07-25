package com.example.partnerlink.application;

import com.example.partnerlink.domain.ApplicationStatus;
import com.example.partnerlink.domain.IllegalStatusTransitionException;
import com.example.partnerlink.domain.MerchantApplication;
import com.example.partnerlink.infrastructure.mybatis.MerchantApplicationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

/**
 * Separate bean so {@code @Transactional} applies on async MUN callbacks
 * (self-invocation inside MerchantApplicationService would bypass the proxy).
 */
@Service
public class ScreeningOutcomeHandler {

    private static final Logger log = LoggerFactory.getLogger(ScreeningOutcomeHandler.class);

    private final MerchantApplicationMapper mapper;

    public ScreeningOutcomeHandler(MerchantApplicationMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public void handle(String applicationId, ApplicationStatus result, Throwable error) {
        MerchantApplication current = mapper.findByApplicationId(applicationId);
        if (current == null) {
            log.warn("Screening callback for unknown applicationId={}", applicationId);
            return;
        }
        if (current.getStatus() != ApplicationStatus.SCREENING) {
            log.info("Ignoring screening callback; applicationId={} already {}", applicationId, current.getStatus());
            return;
        }

        if (error != null) {
            Throwable cause = (error instanceof CompletionException && error.getCause() != null)
                    ? error.getCause() : error;
            if (cause instanceof TimeoutException) {
                log.warn("MUN timeout applicationId={}", applicationId);
                transition(applicationId, ApplicationStatus.SCREENING, ApplicationStatus.TIMEOUT, "MUN screening timed out");
                return;
            }
            log.error("MUN screening failed applicationId={}", applicationId, cause);
            transition(applicationId, ApplicationStatus.SCREENING, ApplicationStatus.TIMEOUT,
                    "MUN screening failed: " + cause.getMessage());
            return;
        }

        if (result == ApplicationStatus.APPROVED || result == ApplicationStatus.REJECTED) {
            transition(applicationId, ApplicationStatus.SCREENING, result, null);
        } else {
            transition(applicationId, ApplicationStatus.SCREENING, ApplicationStatus.TIMEOUT,
                    "Unexpected MUN result: " + result);
        }
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
}
