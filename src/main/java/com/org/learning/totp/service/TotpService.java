package com.org.learning.totp.service;

import com.org.learning.totp.domain.TotpSeed;
import com.org.learning.totp.dto.GenerateTotpResponse;
import com.org.learning.totp.exception.DeviceNotFoundException;
import com.org.learning.totp.exception.QrCodeRenderException;
import com.org.learning.totp.repository.TotpSeedRepository;
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
 * The secret itself is persisted in {@code totp_seed}, keyed by {@code deviceId}, so {@link
 * #validate} looks it up server-side instead of trusting the caller to supply it.
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
    private final TotpSeedRepository seedRepository;

    /** Generates a fresh secret, persists it against {@code deviceId}, and renders its enrollment QR code. */
    public GenerateTotpResponse generate(String deviceId, String issuer) {
        String resolvedIssuer = (issuer == null || issuer.isBlank()) ? DEFAULT_ISSUER : issuer;
        String secret = secretGenerator.generate();

        seedRepository.upsert(deviceId, resolvedIssuer, secret);

        QrData qrData = qrDataFactory.newBuilder().label(deviceId).secret(secret).issuer(resolvedIssuer).build();
        String qrCodeDataUri = renderQrDataUri(qrData);
        log.info("TOTP | secret generated and saved | deviceId={} issuer={}", deviceId, resolvedIssuer);

        return new GenerateTotpResponse(deviceId, resolvedIssuer, secret, qrData.getUri(), qrCodeDataUri);
    }

    /**
     * Looks up the persisted seed for {@code deviceId} and checks the code against it for the
     * current time step (+/- configured discrepancy).
     */
    public boolean validate(String deviceId, String code) {
        TotpSeed seed = seedRepository.findByDeviceId(deviceId);
        if (seed == null) {
            throw new DeviceNotFoundException(deviceId);
        }
        boolean valid = codeVerifier.isValidCode(seed.secret(), code);
        log.info("TOTP | validate | deviceId={} valid={}", deviceId, valid);
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
