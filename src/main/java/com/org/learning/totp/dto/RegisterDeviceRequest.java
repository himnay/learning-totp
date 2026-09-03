package com.org.learning.totp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Request body for {@code POST /api/v1/totp/register}. */
public record RegisterDeviceRequest(
    @Schema(
                description =
                        "Identifier for this MFA app install — a device can host more than one, so this "
                                + "is the app instance, not the device itself",
                example = "alice-iphone-15-authenticator-v2")
        @NotBlank(message = "appId must not be blank")
        String appId,
    @Schema(
            description = "Issuer shown alongside the app id in the authenticator app",
            example = "learning-totp",
            defaultValue = "learning-totp")
        String issuer) {}
