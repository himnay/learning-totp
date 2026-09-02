package com.org.learning.totp.controller;

import com.org.learning.totp.dto.GenerateRecoveryCodesRequest;
import com.org.learning.totp.dto.GenerateRecoveryCodesResponse;
import com.org.learning.totp.dto.GenerateTotpRequest;
import com.org.learning.totp.dto.GenerateTotpResponse;
import com.org.learning.totp.dto.ValidateRecoveryCodeRequest;
import com.org.learning.totp.dto.ValidateRecoveryCodeResponse;
import com.org.learning.totp.dto.ValidateTotpRequest;
import com.org.learning.totp.dto.ValidateTotpResponse;
import com.org.learning.totp.service.RecoveryCodeService;
import com.org.learning.totp.service.TotpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/totp} — generate/validate TOTP codes and one-time recovery codes. */
@RestController
@Tag(name = "TOTP")
@RequestMapping("/api/v1/totp")
@RequiredArgsConstructor
public class TotpController {

    private final TotpService totpService;
    private final RecoveryCodeService recoveryCodeService;

    @PostMapping("/generate")
    @Operation(
            operationId = "generateTotp",
            summary = "Generate a new TOTP secret and enrollment QR code",
            description =
                    "Creates a fresh Base32 secret via the auto-configured SecretGenerator, persists it "
                            + "against deviceId, builds an otpauth:// URI via QrDataFactory, and renders it as a QR "
                            + "code PNG (as a base64 data URI) via QrGenerator. Calling this again for the same "
                            + "deviceId rotates the secret.")
    public GenerateTotpResponse generate(@Valid @RequestBody GenerateTotpRequest request) {
        return totpService.generate(request.deviceId(), request.issuer());
    }

    @PostMapping("/validate")
    @Operation(
            operationId = "validateTotp",
            summary = "Validate a code against the secret saved for a device",
            description =
                    "Looks up the persisted seed for deviceId and recomputes the expected code for the "
                            + "current time step (and the configured +/- discrepancy window) via the "
                            + "auto-configured CodeVerifier.")
    public ValidateTotpResponse validate(@Valid @RequestBody ValidateTotpRequest request) {
        return new ValidateTotpResponse(totpService.validate(request.deviceId(), request.code()));
    }

    @PostMapping("/recovery-codes/generate")
    @Operation(
            operationId = "generateRecoveryCodes",
            summary = "Generate one-time recovery codes for an already-enrolled device",
            description =
                    "Uses the auto-configured RecoveryCodeGenerator to produce a batch of 16-character "
                            + "codes (default 10, max 20), replacing any previously issued batch for this "
                            + "deviceId. Requires that /generate has already been called for this deviceId. "
                            + "Codes are returned once — store them client-side.")
    public GenerateRecoveryCodesResponse generateRecoveryCodes(
            @Valid @RequestBody GenerateRecoveryCodesRequest request) {
        return new GenerateRecoveryCodesResponse(
                request.deviceId(), recoveryCodeService.generate(request.deviceId(), request.count()));
    }

    @PostMapping("/recovery-codes/validate")
    @Operation(
            operationId = "validateRecoveryCode",
            summary = "Redeem a one-time recovery code",
            description =
                    "Atomically marks the code used if — and only if — it matches an unredeemed code "
                            + "issued for deviceId. Returns false for a wrong code, an already-used code, or an "
                            + "unknown device, deliberately without distinguishing which.")
    public ValidateRecoveryCodeResponse validateRecoveryCode(@Valid @RequestBody ValidateRecoveryCodeRequest request) {
        return new ValidateRecoveryCodeResponse(
                recoveryCodeService.validate(request.deviceId(), request.code()));
    }
}
