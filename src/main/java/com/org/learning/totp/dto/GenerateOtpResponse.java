package com.org.learning.totp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Response body for {@code POST /api/v1/totp/generate-code}. */
@Schema(description = "The current numeric TOTP code for an already-registered app's persisted secret")
public record GenerateOtpResponse(
    @Schema(example = "alice-iphone-15-authenticator-v2") String appId,
    @Schema(description = "The code for the current time step", example = "482913") String code,
    @Schema(description = "Seconds left before this code expires and a new one takes over", example = "42")
        int validForSeconds) {}
