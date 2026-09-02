package com.org.learning.totp.web;

import com.org.learning.totp.service.TotpService;
import com.org.learning.totp.web.dto.GenerateTotpRequest;
import com.org.learning.totp.web.dto.GenerateTotpResponse;
import com.org.learning.totp.web.dto.ValidateTotpRequest;
import com.org.learning.totp.web.dto.ValidateTotpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/totp} — generate and validate RFC 6238 time-based one-time codes. */
@RestController
@Tag(name = "TOTP")
@RequestMapping("/api/v1/totp")
@RequiredArgsConstructor
public class TotpController {

    private final TotpService totpService;

    @PostMapping("/generate")
    @Operation(
            operationId = "generateTotp",
            summary = "Generate a new TOTP secret and enrollment QR code",
            description =
                    "Creates a fresh Base32 secret via the auto-configured SecretGenerator, builds an "
                            + "otpauth:// URI via QrDataFactory, and renders it as a QR code PNG (as a base64 data "
                            + "URI) via QrGenerator. Nothing is persisted — the caller is responsible for storing "
                            + "the secret against the account.")
    public GenerateTotpResponse generate(@Valid @RequestBody GenerateTotpRequest request) {
        return totpService.generate(request.accountName(), request.issuer());
    }

    @PostMapping("/validate")
    @Operation(
            operationId = "validateTotp",
            summary = "Validate a code against a secret",
            description =
                    "Recomputes the expected code for the current time step (and the configured +/- "
                            + "discrepancy window) via the auto-configured CodeVerifier and compares it against "
                            + "the submitted code.")
    public ValidateTotpResponse validate(@Valid @RequestBody ValidateTotpRequest request) {
        return new ValidateTotpResponse(totpService.validate(request.secret(), request.code()));
    }
}
