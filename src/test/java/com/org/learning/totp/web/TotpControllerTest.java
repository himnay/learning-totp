package com.org.learning.totp.web;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full-context test: exercises the real beans that {@code totp-spring-boot-starter}
 * auto-configures (no mocks), proving the generate -> validate round trip actually works.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TotpControllerTest {

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
                        .content("{\"accountName\":\"alice@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountName").value("alice@example.com"))
                .andExpect(jsonPath("$.issuer").value("learning-totp"))
                .andExpect(jsonPath("$.secret").value(matchesPattern("[A-Z2-7]{32}")))
                .andExpect(jsonPath("$.otpAuthUri").value(startsWith("otpauth://totp/")))
                .andExpect(jsonPath("$.qrCodeDataUri").value(startsWith("data:image/png;base64,")));
    }

    @Test
    void generateThenValidateWithTheCurrentCodeSucceeds() throws Exception {
        String generateResponse = mockMvc.perform(post("/api/v1/totp/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountName\":\"bob@example.com\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secret = JsonPath.read(generateResponse, "$.secret");
        String currentCode = codeGenerator.generate(secret, timeProvider.getTime() / 30);

        mockMvc.perform(post("/api/v1/totp/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secret\":\"" + secret + "\",\"code\":\"" + currentCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void validateWithAWrongCodeReturnsFalseNotAnError() throws Exception {
        mockMvc.perform(post("/api/v1/totp/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secret\":\"JBSWY3DPEHPK3PXP\",\"code\":\"000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void generateWithABlankAccountNameReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/totp/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
