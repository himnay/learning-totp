package com.org.learning.totp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Response body for {@code POST /api/v1/totp/recovery-codes/generate}. */
@Schema(description = "A freshly generated set of one-time recovery codes — shown once, never retrievable again")
public record GenerateRecoveryCodesResponse(
    @Schema(example = "alice-iphone-15") String deviceId,
    @Schema(description = "16-character codes (numbers + lowercase letters, dash-grouped), each valid for exactly one use")
        List<String> codes) {}
