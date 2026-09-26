package com.org.learning.totp.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.time.TimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full-context test against a real Postgres (Testcontainers): exercises the actual beans that
 * {@code totp-spring-boot-starter} auto-configures plus the JDBC/Flyway persistence layer — no
 * mocks anywhere in this class.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class TotpControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CodeGenerator codeGenerator;

    @Autowired
    private TimeProvider timeProvider;

    @Value("${totp.time.period}")
    private int timePeriod;

    @Test
    void registerReturnsASecretAndAnOtpAuthUri() throws Exception {
        mockMvc.perform(post("/api/v1/totp/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"alice-iphone-15-authenticator-v2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value("alice-iphone-15-authenticator-v2"))
                .andExpect(jsonPath("$.issuer").value("learning-totp"))
                .andExpect(jsonPath("$.secret").value(matchesPattern("[A-Z2-7]{32}")))
                .andExpect(jsonPath("$.otpAuthUri").value(startsWith("otpauth://totp/")));
    }

    @Test
    void generateQrForARegisteredAppReturnsAScannableQrCode() throws Exception {
        mockMvc.perform(post("/api/v1/totp/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"erin-macbook-authenticator\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/totp/generate-qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"erin-macbook-authenticator\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value("erin-macbook-authenticator"))
                .andExpect(jsonPath("$.otpAuthUri").value(startsWith("otpauth://totp/")))
                .andExpect(jsonPath("$.qrCodeDataUri").value(startsWith("data:image/png;base64,")));
    }

    @Test
    void generateQrForAnUnregisteredAppReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/totp/generate-qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"never-registered\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateReturnsTheCurrentNumericCodeAndItValidatesSuccessfully() throws Exception {
        String registerResponse = mockMvc.perform(post("/api/v1/totp/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"bob-pixel-9-authenticator\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String secret = JsonPath.read(registerResponse, "$.secret");

        String generateResponse = mockMvc.perform(post("/api/v1/totp/generate-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"bob-pixel-9-authenticator\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value("bob-pixel-9-authenticator"))
                .andExpect(jsonPath("$.code").value(matchesPattern("\\d{6}")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String expectedCode = codeGenerator.generate(secret, timeProvider.getTime() / timePeriod);
        String returnedCode = JsonPath.read(generateResponse, "$.code");
        org.assertj.core.api.Assertions.assertThat(returnedCode).isEqualTo(expectedCode);

        mockMvc.perform(post("/api/v1/totp/validate-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"bob-pixel-9-authenticator\",\"code\":\"" + returnedCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void validateQrAndValidateAgreeOnTheSameCode() throws Exception {
        // Both endpoints run the same check; each gets its own app, because a code is single-use.
        for (String endpoint : new String[]{"/api/v1/totp/validate-qr", "/api/v1/totp/validate-code"}) {
            String appId = "frank-tablet" + endpoint.replace('/', '-');
            String secret = JsonPath.read(mockMvc.perform(post("/api/v1/totp/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"appId\":\"" + appId + "\"}"))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString(),
                    "$.secret");
            String currentCode = codeGenerator.generate(secret, timeProvider.getTime() / timePeriod);

            mockMvc.perform(post(endpoint)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"appId\":\"" + appId + "\",\"code\":\"" + currentCode + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true));
        }
    }

    @Test
    void aCodeCannotBeReplayed() throws Exception {
        String secret = JsonPath.read(mockMvc.perform(post("/api/v1/totp/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"appId\":\"mallory-replay-authenticator\"}"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.secret");
        String code = codeGenerator.generate(secret, timeProvider.getTime() / timePeriod);
        String body = "{\"appId\":\"mallory-replay-authenticator\",\"code\":\"" + code + "\"}";

        mockMvc.perform(post("/api/v1/totp/validate-code").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
        // RFC 6238 §5.2 — the same OTP is rejected after it has been accepted once.
        mockMvc.perform(post("/api/v1/totp/validate-qr").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void validateWithAWrongCodeReturnsFalseNotAnError() throws Exception {
        mockMvc.perform(post("/api/v1/totp/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"carol-galaxy-s25-authenticator\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/totp/validate-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"carol-galaxy-s25-authenticator\",\"code\":\"000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void validateForAnUnregisteredAppReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/totp/validate-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"never-registered\",\"code\":\"123456\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void registerWithABlankAppIdReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/totp/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void recoveryCodesCanBeGeneratedAndEachRedeemedExactlyOnce() throws Exception {
        mockMvc.perform(post("/api/v1/totp/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"dave-ipad-authenticator\"}"))
                .andExpect(status().isOk());

        String generateResponse = mockMvc.perform(post("/api/v1/totp/recovery-codes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"dave-ipad-authenticator\",\"count\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value("dave-ipad-authenticator"))
                .andExpect(jsonPath("$.codes", hasSize(3)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstCode = JsonPath.read(generateResponse, "$.codes[0]");

        mockMvc.perform(post("/api/v1/totp/recovery-codes/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"dave-ipad-authenticator\",\"code\":\"" + firstCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        // second attempt with the same code fails — one-time use
        mockMvc.perform(post("/api/v1/totp/recovery-codes/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"dave-ipad-authenticator\",\"code\":\"" + firstCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void generatingRecoveryCodesForAnUnregisteredAppReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/totp/recovery-codes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appId\":\"never-registered\"}"))
                .andExpect(status().isNotFound());
    }
}
