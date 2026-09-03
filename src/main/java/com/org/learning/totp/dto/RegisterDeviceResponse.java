package com.org.learning.totp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Response body for {@code POST /api/v1/totp/register}. */
@Schema(description = "A freshly generated TOTP secret, persisted server-side against appId")
public record RegisterDeviceResponse(
    @Schema(example = "alice-iphone-15-authenticator-v2") String appId,
    @Schema(example = "learning-totp") String issuer,
    @Schema(description = "Base32-encoded shared secret, also persisted server-side against appId", example = "JBSWY3DPEHPK3PXP")
        String secret,
    @Schema(
            description = "otpauth:// provisioning URI — for manual entry, or feed it to /generate-qr for a scannable code",
            example = "otpauth://totp/learning-totp:alice-iphone-15-authenticator-v2?secret=JBSWY3DPEHPK3PXP&issuer=learning-totp&algorithm=SHA1&digits=6&period=60")
        String otpAuthUri) {}
