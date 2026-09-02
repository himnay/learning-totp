package com.org.learning.totp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request body for {@code POST /api/v1/totp/validate-code} and {@code POST /api/v1/totp/validate-qr}. */
public record ValidateTotpRequest(
    @Schema(description = "The app id the secret was registered for", example = "alice-iphone-15-authenticator-v2")
        @NotBlank(message = "appId must not be blank")
        String appId,
    @Schema(description = "The code currently displayed in the authenticator app", example = "123456")
        @NotBlank(message = "code must not be blank")
        @Pattern(regexp = "\\d+", message = "code must be numeric")
        String code) {}
