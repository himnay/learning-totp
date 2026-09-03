package com.org.learning.totp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Response body for {@code POST /api/v1/totp/generate-qr}. */
@Schema(description = "The enrollment QR code for an already-registered app's persisted secret")
public record GenerateQrResponse(
    @Schema(example = "alice-iphone-15-authenticator-v2") String appId,
    @Schema(example = "learning-totp") String issuer,
    @Schema(
            description = "otpauth:// provisioning URI, as encoded into the QR code",
            example = "otpauth://totp/learning-totp:alice-iphone-15-authenticator-v2?secret=JBSWY3DPEHPK3PXP&issuer=learning-totp&algorithm=SHA1&digits=6&period=60")
        String otpAuthUri,
    @Schema(description = "The same URI rendered as a scannable QR code, embedded as a base64 PNG data URI")
        String qrCodeDataUri) {}
