package com.example.partnerlink.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationStatusTest {

    @Test
    void allowsHappyPath() {
        assertTrue(ApplicationStatus.APPLIED.canTransitionTo(ApplicationStatus.SCREENING));
        assertTrue(ApplicationStatus.SCREENING.canTransitionTo(ApplicationStatus.APPROVED));
        assertTrue(ApplicationStatus.SCREENING.canTransitionTo(ApplicationStatus.REJECTED));
        assertTrue(ApplicationStatus.SCREENING.canTransitionTo(ApplicationStatus.TIMEOUT));
    }

    @Test
    void rejectsIllegalTransitions() {
        assertFalse(ApplicationStatus.APPROVED.canTransitionTo(ApplicationStatus.SCREENING));
        assertFalse(ApplicationStatus.APPLIED.canTransitionTo(ApplicationStatus.APPROVED));
        assertFalse(ApplicationStatus.REJECTED.canTransitionTo(ApplicationStatus.APPROVED));

        assertThrows(IllegalStatusTransitionException.class,
                () -> ApplicationStatus.APPROVED.assertCanTransitionTo(ApplicationStatus.SCREENING));
    }

    @Test
    void allowsRescreenAfterTimeout() {
        assertDoesNotThrow(() -> ApplicationStatus.TIMEOUT.assertCanTransitionTo(ApplicationStatus.SCREENING));
    }
}
