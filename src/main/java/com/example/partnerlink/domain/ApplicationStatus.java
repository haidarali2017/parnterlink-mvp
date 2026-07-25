package com.example.partnerlink.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Merchant application lifecycle. Illegal transitions are rejected.
 */
public enum ApplicationStatus {
    APPLIED,
    SCREENING,
    APPROVED,
    REJECTED,
    TIMEOUT;

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED = Map.of(
            APPLIED, EnumSet.of(SCREENING),
            SCREENING, EnumSet.of(APPROVED, REJECTED, TIMEOUT),
            APPROVED, EnumSet.noneOf(ApplicationStatus.class),
            REJECTED, EnumSet.noneOf(ApplicationStatus.class),
            TIMEOUT, EnumSet.of(SCREENING) // allow re-screen after timeout
    );

    public boolean canTransitionTo(ApplicationStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public void assertCanTransitionTo(ApplicationStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStatusTransitionException(this, target);
        }
    }
}
