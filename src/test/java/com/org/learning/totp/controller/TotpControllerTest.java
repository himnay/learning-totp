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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
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
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CodeGenerator codeGenerator;

    @Autowired
    private TimeProvider timeProvider;

    @Test
    void generateReturnsASecretAndAScannableQrCode() throws Exception {
        mockMvc.perform(post("/api/v1/totp/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"alice-iphone-15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("alice-iphone-15"))
                .andExpect(jsonPath("$.issuer").value("learning-totp"))
                .andExpect(jsonPath("$.secret").value(matchesPattern("[A-Z2-7]{32}")))
                .andExpect(jsonPath("$.otpAuthUri").value(startsWith("otpauth://totp/")))
                .andExpect(jsonPath("$.qrCodeDataUri").value(startsWith("data:image/png;base64,")));
    }

    @Test
    void generateThenValidateWithTheCurrentCodeSucceeds() throws Exception {
        String generateResponse = mockMvc.perform(post("/api/v1/totp/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"bob-pixel-9\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secret = JsonPath.read(generateResponse, "$.secret");
        String currentCode = codeGenerator.generate(secret, timeProvider.getTime() / 30);

        mockMvc.perform(post("/api/v1/totp/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"bob-pixel-9\",\"code\":\"" + currentCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void validateWithAWrongCodeReturnsFalseNotAnError() throws Exception {
        mockMvc.perform(post("/api/v1/totp/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"carol-galaxy-s25\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/totp/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"carol-galaxy-s25\",\"code\":\"000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void validateForAnUnknownDeviceReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/totp/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"never-enrolled\",\"code\":\"123456\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateWithABlankDeviceIdReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/totp/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void recoveryCodesCanBeGeneratedAndEachRedeemedExactlyOnce() throws Exception {
        mockMvc.perform(post("/api/v1/totp/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"dave-ipad\"}"))
                .andExpect(status().isOk());

        String generateResponse = mockMvc.perform(post("/api/v1/totp/recovery-codes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"dave-ipad\",\"count\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("dave-ipad"))
                .andExpect(jsonPath("$.codes", hasSize(3)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String firstCode = JsonPath.read(generateResponse, "$.codes[0]");

        mockMvc.perform(post("/api/v1/totp/recovery-codes/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"dave-ipad\",\"code\":\"" + firstCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        // second attempt with the same code fails — one-time use
        mockMvc.perform(post("/api/v1/totp/recovery-codes/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"dave-ipad\",\"code\":\"" + firstCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void generatingRecoveryCodesForAnUnenrolledDeviceReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/totp/recovery-codes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"never-enrolled\"}"))
                .andExpect(status().isNotFound());
    }
}
