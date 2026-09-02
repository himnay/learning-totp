package com.org.learning.totp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Request body for {@code POST /api/v1/totp/generate}. */
public record GenerateTotpRequest(
    @Schema(description = "Device identifier the secret is persisted against", example = "alice-iphone-15")
        @NotBlank(message = "deviceId must not be blank")
        String deviceId,
    @Schema(
            description = "Issuer shown alongside the device id in the authenticator app",
            example = "learning-totp",
            defaultValue = "learning-totp")
        String issuer) {}
