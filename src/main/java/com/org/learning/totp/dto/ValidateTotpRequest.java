package com.org.learning.totp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request body for {@code POST /api/v1/totp/validate}. */
public record ValidateTotpRequest(
    @Schema(description = "The device id the secret was generated for", example = "alice-iphone-15")
        @NotBlank(message = "deviceId must not be blank")
        String deviceId,
    @Schema(description = "The code currently displayed in the authenticator app", example = "123456")
        @NotBlank(message = "code must not be blank")
        @Pattern(regexp = "\\d+", message = "code must be numeric")
        String code) {}
