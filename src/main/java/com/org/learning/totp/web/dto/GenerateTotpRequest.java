package com.org.learning.totp.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Request body for {@code POST /api/v1/totp/generate}. */
public record GenerateTotpRequest(
    @Schema(description = "Account/label shown in the authenticator app", example = "alice@example.com")
        @NotBlank(message = "accountName must not be blank")
        String accountName,
    @Schema(
            description = "Issuer shown alongside the account name in the authenticator app",
            example = "learning-totp",
            defaultValue = "learning-totp")
        String issuer) {}
