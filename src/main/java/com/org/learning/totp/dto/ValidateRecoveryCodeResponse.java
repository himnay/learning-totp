package com.org.learning.totp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Response body for {@code POST /api/v1/totp/recovery-codes/validate}. */
public record ValidateRecoveryCodeResponse(
    @Schema(description = "true only the first time a given unused code is submitted") boolean valid) {}
