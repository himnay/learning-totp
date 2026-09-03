package com.org.learning.totp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Request body for {@code POST /api/v1/totp/generate-qr} and {@code POST /api/v1/totp/generate-code}. */
public record AppIdRequest(
    @Schema(description = "The app id /register was called with", example = "alice-iphone-15-authenticator-v2")
        @NotBlank(message = "appId must not be blank")
        String appId) {}
