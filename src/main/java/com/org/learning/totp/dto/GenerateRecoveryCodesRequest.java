package com.org.learning.totp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Request body for {@code POST /api/v1/totp/recovery-codes/generate}. */
public record GenerateRecoveryCodesRequest(
    @Schema(description = "App id an enrolled TOTP secret already exists for", example = "alice-iphone-15-authenticator-v2")
        @NotBlank(message = "appId must not be blank")
        String appId,
    @Schema(description = "How many codes to generate — replaces any previously issued codes for this app", example = "10", defaultValue = "10")
        @Min(value = 1, message = "count must be at least 1")
        @Max(value = 20, message = "count must be at most 20")
        Integer count) {}
