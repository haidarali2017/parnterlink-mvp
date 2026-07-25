package com.example.partnerlink;

import com.example.partnerlink.application.MerchantApplicationService;
import com.example.partnerlink.domain.ApplicationStatus;
import com.example.partnerlink.domain.MerchantApplication;
import com.example.partnerlink.infrastructure.mun.MockMunClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
class TimeoutTest {

    @DynamicPropertySource
    static void munTimeoutProps(DynamicPropertyRegistry registry) {
        // MUN takes longer than the client-side timeout → TIMEOUT status, never APPROVED
        registry.add("partnerlink.mun.delay-ms", () -> "2000");
        registry.add("partnerlink.mun.timeout-ms", () -> "300");
        registry.add("partnerlink.mun.default-result", () -> "APPROVED");
    }

    @Autowired
    MerchantApplicationService service;

    @Autowired
    MockMunClient mockMunClient;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("DELETE FROM merchant_application");
        mockMunClient.resetCallCount();
    }

    @Test
    void screeningTimeoutDoesNotApprove() {
        String id = UUID.randomUUID().toString();
        service.apply(id, "Slow Shop");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            MerchantApplication app = service.getByApplicationId(id);
            assertThat(app.getStatus()).isEqualTo(ApplicationStatus.TIMEOUT);
            assertThat(app.getFailureReason()).containsIgnoringCase("timed out");
        });

        MerchantApplication finalApp = service.getByApplicationId(id);
        assertThat(finalApp.getStatus()).isNotEqualTo(ApplicationStatus.APPROVED);
    }
}
