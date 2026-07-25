package com.example.partnerlink;

import com.example.partnerlink.api.dto.ApplyRequest;
import com.example.partnerlink.domain.ApplicationStatus;
import com.example.partnerlink.infrastructure.mun.MockMunClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdempotencyTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

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
    void duplicateApplyReturnsSameMerchantNumberAndDoesNotCallMunTwice() throws Exception {
        String applicationId = UUID.randomUUID().toString();
        ApplyRequest body = new ApplyRequest();
        body.setApplicationId(applicationId);
        body.setMerchantName("Demo Shop");

        MvcResult first = mockMvc.perform(post("/merchants/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SCREENING"))
                .andExpect(jsonPath("$.merchantNumber").isNotEmpty())
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(first.getResponse().getContentAsString());
        String merchantNumber = firstJson.get("merchantNumber").asText();

        // Allow afterCommit to schedule the first MUN call
        await().atMost(2, TimeUnit.SECONDS).until(() -> mockMunClient.getCallCount() >= 1);

        mockMvc.perform(post("/merchants/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId))
                .andExpect(jsonPath("$.merchantNumber").value(merchantNumber));

        assertThat(mockMunClient.getCallCount()).isEqualTo(1);

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM merchant_application WHERE application_id = ?",
                Integer.class,
                applicationId);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void getReturnsApplication() throws Exception {
        String applicationId = UUID.randomUUID().toString();
        ApplyRequest body = new ApplyRequest();
        body.setApplicationId(applicationId);
        body.setMerchantName("Lookup Shop");

        mockMvc.perform(post("/merchants/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/merchants/{id}", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantName").value("Lookup Shop"))
                .andExpect(jsonPath("$.status").value(ApplicationStatus.SCREENING.name()));
    }
}
