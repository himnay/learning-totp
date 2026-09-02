package com.org.learning.totp.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request body for {@code POST /api/v1/totp/validate}. */
public record ValidateTotpRequest(
    @Schema(description = "The Base32 secret returned by /generate", example = "JBSWY3DPEHPK3PXP")
        @NotBlank(message = "secret must not be blank")
        String secret,
    @Schema(description = "The code currently displayed in the authenticator app", example = "123456")
        @NotBlank(message = "code must not be blank")
        @Pattern(regexp = "\\d+", message = "code must be numeric")
        String code) {}
