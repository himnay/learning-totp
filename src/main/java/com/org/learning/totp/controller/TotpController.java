package com.org.learning.totp.controller;

import com.org.learning.totp.dto.AppIdRequest;
import com.org.learning.totp.dto.GenerateOtpResponse;
import com.org.learning.totp.dto.GenerateQrResponse;
import com.org.learning.totp.dto.GenerateRecoveryCodesRequest;
import com.org.learning.totp.dto.GenerateRecoveryCodesResponse;
import com.org.learning.totp.dto.RegisterDeviceRequest;
import com.org.learning.totp.dto.RegisterDeviceResponse;
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

/** {@code /api/v1/totp} — register apps, generate/validate TOTP codes, and one-time recovery codes. */
@RestController
@Tag(name = "TOTP")
@RequestMapping("/api/v1/totp")
@RequiredArgsConstructor
public class TotpController {

    private final TotpService totpService;
    private final RecoveryCodeService recoveryCodeService;

    @PostMapping("/register")
    @Operation(
            operationId = "registerDevice",
            summary = "Register an MFA app install and generate its TOTP secret",
            description =
                    "Creates a fresh Base32 secret via the auto-configured SecretGenerator and persists it "
                            + "against appId — an MFA app install, since one device can host several. Calling "
                            + "this again for the same appId rotates the secret. Required before /generate-qr, "
                            + "/generate, /validate-qr, /validate or /recovery-codes/generate will work for it.")
    public RegisterDeviceResponse register(@Valid @RequestBody RegisterDeviceRequest request) {
        return totpService.register(request.appId(), request.issuer());
    }

    @PostMapping("/generate-qr")
    @Operation(
            operationId = "generateQr",
            summary = "Render the enrollment QR code for an already-registered app",
            description =
                    "Looks up the secret persisted for appId, builds an otpauth:// URI via QrDataFactory, "
                            + "and renders it as a QR code PNG (as a base64 data URI) via QrGenerator.")
    public GenerateQrResponse generateQr(@Valid @RequestBody AppIdRequest request) {
        return totpService.generateQr(request.appId());
    }

    @PostMapping("/generate-code")
    @Operation(
            operationId = "generateOtp",
            summary = "Generate the current numeric TOTP code for an already-registered app",
            description =
                    "Looks up the secret persisted for appId and computes the code for the current time "
                            + "step via the auto-configured CodeGenerator, alongside how many seconds remain "
                            + "before it rotates.")
    public GenerateOtpResponse generateCode(@Valid @RequestBody AppIdRequest request) {
        return totpService.generateOtp(request.appId());
    }

    @PostMapping("/validate-qr")
    @Operation(
            operationId = "validateQrEnrolledCode",
            summary = "Validate a code from an app enrolled via /generate-qr",
            description =
                    "Looks up the persisted seed for appId and recomputes the expected code for the "
                            + "current time step (and the configured +/- discrepancy window) via the "
                            + "auto-configured CodeVerifier.")
    public ValidateTotpResponse validateQr(@Valid @RequestBody ValidateTotpRequest request) {
        return new ValidateTotpResponse(totpService.validate(request.appId(), request.code()));
    }

    @PostMapping("/validate-code")
    @Operation(
            operationId = "validateTotp",
            summary = "Validate a code against the secret saved for an app",
            description =
                    "Identical check to /validate-qr — validation doesn't depend on how the app was "
                            + "enrolled, only on the secret persisted for appId.")
    public ValidateTotpResponse validateCode(@Valid @RequestBody ValidateTotpRequest request) {
        return new ValidateTotpResponse(totpService.validate(request.appId(), request.code()));
    }

    @PostMapping("/recovery-codes/generate")
    @Operation(
            operationId = "generateRecoveryCodes",
            summary = "Generate one-time recovery codes for an already-registered app",
            description =
                    "Uses the auto-configured RecoveryCodeGenerator to produce a batch of 16-character "
                            + "codes (default 10, max 20), replacing any previously issued batch for this "
                            + "appId. Requires that /register has already been called for this appId. "
                            + "Codes are returned once — store them client-side.")
    public GenerateRecoveryCodesResponse generateRecoveryCodes(
            @Valid @RequestBody GenerateRecoveryCodesRequest request) {
        return new GenerateRecoveryCodesResponse(
                request.appId(), recoveryCodeService.generate(request.appId(), request.count()));
    }

    @PostMapping("/recovery-codes/validate")
    @Operation(
            operationId = "validateRecoveryCode",
            summary = "Redeem a one-time recovery code",
            description =
                    "Atomically marks the code used if — and only if — it matches an unredeemed code "
                            + "issued for appId. Returns false for a wrong code, an already-used code, or an "
                            + "unknown app, deliberately without distinguishing which.")
    public ValidateRecoveryCodeResponse validateRecoveryCode(@Valid @RequestBody ValidateRecoveryCodeRequest request) {
        return new ValidateRecoveryCodeResponse(
                recoveryCodeService.validate(request.appId(), request.code()));
    }
}
