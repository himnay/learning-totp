package com.org.learning.totp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Request body for {@code POST /api/v1/totp/recovery-codes/validate}. */
public record ValidateRecoveryCodeRequest(
    @Schema(example = "alice-iphone-15") @NotBlank(message = "deviceId must not be blank") String deviceId,
    @Schema(description = "One of the codes returned by /recovery-codes/generate", example = "tf8i-exmo-3lcb-slkm")
        @NotBlank(message = "code must not be blank")
        String code) {}
