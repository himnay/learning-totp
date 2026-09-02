package com.org.learning.totp.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Response body for {@code POST /api/v1/totp/generate}. */
@Schema(description = "A freshly generated TOTP secret plus everything needed to enroll it in an authenticator app")
public record GenerateTotpResponse(
    @Schema(example = "alice@example.com") String accountName,
    @Schema(example = "learning-totp") String issuer,
    @Schema(description = "Base32-encoded shared secret — never returned again after this call", example = "JBSWY3DPEHPK3PXP")
        String secret,
    @Schema(
            description = "otpauth:// provisioning URI, as encoded into the QR code",
            example = "otpauth://totp/learning-totp:alice%40example.com?secret=JBSWY3DPEHPK3PXP&issuer=learning-totp&algorithm=SHA1&digits=6&period=30")
        String otpAuthUri,
    @Schema(description = "The same URI rendered as a scannable QR code, embedded as a base64 PNG data URI")
        String qrCodeDataUri) {}
