package com.org.learning.totp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Response body for {@code POST /api/v1/totp/validate-code} and {@code POST /api/v1/totp/validate-qr}. */
public record ValidateTotpResponse(@Schema(example = "true") boolean valid) {}
