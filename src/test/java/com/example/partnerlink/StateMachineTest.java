package com.example.partnerlink;

import com.example.partnerlink.application.MerchantApplicationService;
import com.example.partnerlink.domain.ApplicationStatus;
import com.example.partnerlink.domain.IllegalStatusTransitionException;
import com.example.partnerlink.domain.MerchantApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class StateMachineTest {

    @Autowired
    MerchantApplicationService service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("DELETE FROM merchant_application");
    }

    @Test
    void applyStartsInScreening() {
        String id = UUID.randomUUID().toString();
        MerchantApplication app = service.apply(id, "State Shop").application();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.SCREENING);
    }

    @Test
    void rejectsIllegalTransitionFromApprovedBackToScreening() {
        String id = UUID.randomUUID().toString();
        service.apply(id, "State Shop");

        // Simulate completed screening
        jdbcTemplate.update(
                "UPDATE merchant_application SET status = ? WHERE application_id = ?",
                ApplicationStatus.APPROVED.name(), id);

        assertThatThrownBy(() -> service.transitionStatus(id, ApplicationStatus.SCREENING))
                .isInstanceOf(IllegalStatusTransitionException.class)
                .hasMessageContaining("APPROVED")
                .hasMessageContaining("SCREENING");
    }

    @Test
    void rejectsSkippingScreening() {
        String id = UUID.randomUUID().toString();
        // Insert stuck in APPLIED without going through service transitions
        jdbcTemplate.update(
                "INSERT INTO merchant_application (application_id, merchant_name, status) VALUES (?, ?, ?)",
                id, "Skip Shop", ApplicationStatus.APPLIED.name());

        assertThatThrownBy(() -> service.transitionStatus(id, ApplicationStatus.APPROVED))
                .isInstanceOf(IllegalStatusTransitionException.class);
    }
}
