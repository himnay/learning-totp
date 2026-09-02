package com.org.learning.totp.service;

import com.org.learning.totp.exception.QrCodeRenderException;
import com.org.learning.totp.web.dto.GenerateTotpResponse;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Generates and verifies TOTP codes using only beans that {@code totp-spring-boot-starter}
 * auto-configures — no {@code new Default...()} wiring, unlike a plain {@code totp} dependency.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TotpService {

    private static final String DEFAULT_ISSUER = "learning-totp";

    private final SecretGenerator secretGenerator;
    private final QrDataFactory qrDataFactory;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;

    /** Generates a fresh secret and its scannable enrollment QR code. Nothing is persisted. */
    public GenerateTotpResponse generate(String accountName, String issuer) {
        String resolvedIssuer = (issuer == null || issuer.isBlank()) ? DEFAULT_ISSUER : issuer;
        String secret = secretGenerator.generate();

        QrData qrData = qrDataFactory.newBuilder().label(accountName).secret(secret).issuer(resolvedIssuer).build();

        String qrCodeDataUri = renderQrDataUri(qrData);
        log.info("TOTP | secret generated | accountName={} issuer={}", accountName, resolvedIssuer);

        return new GenerateTotpResponse(accountName, resolvedIssuer, secret, qrData.getUri(), qrCodeDataUri);
    }

    /** Checks a submitted code against the secret for the current time step (+/- configured discrepancy). */
    public boolean validate(String secret, String code) {
        boolean valid = codeVerifier.isValidCode(secret, code);
        log.info("TOTP | validate | valid={}", valid);
        return valid;
    }

    private String renderQrDataUri(QrData qrData) {
        try {
            byte[] image = qrGenerator.generate(qrData);
            return Utils.getDataUriForImage(image, qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            throw new QrCodeRenderException("Failed to render the enrollment QR code", e);
        }
    }
}
