package com.org.learning.totp.service;

import com.org.learning.totp.domain.TotpSeed;
import com.org.learning.totp.dto.GenerateOtpResponse;
import com.org.learning.totp.dto.GenerateQrResponse;
import com.org.learning.totp.dto.RegisterDeviceResponse;
import com.org.learning.totp.exception.AppNotFoundException;
import com.org.learning.totp.exception.OtpGenerationException;
import com.org.learning.totp.exception.QrCodeRenderException;
import com.org.learning.totp.repository.TotpSeedRepository;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Registers and verifies TOTP-enrolled apps using only beans that {@code totp-spring-boot-starter}
 * auto-configures — no {@code new Default...()} wiring, unlike a plain {@code totp} dependency.
 * The secret is persisted in {@code totp_seed}, keyed by {@code appId} (one MFA app install — a
 * device can host several), so every method here looks it up server-side instead of trusting the
 * caller to supply it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TotpService {

    private static final String DEFAULT_ISSUER = "learning-totp";

    private final SecretGenerator secretGenerator;
    private final QrDataFactory qrDataFactory;
    private final QrGenerator qrGenerator;
    private final CodeGenerator codeGenerator;
    private final CodeVerifier codeVerifier;
    private final TimeProvider timeProvider;
    private final TotpSeedRepository seedRepository;

    @Value("${totp.time.period}")
    private int timePeriod;

    /** Generates a fresh secret and persists it against {@code appId} (re-registering rotates the secret). */
    public RegisterDeviceResponse register(String appId, String issuer) {
        String resolvedIssuer = (issuer == null || issuer.isBlank()) ? DEFAULT_ISSUER : issuer;
        String secret = secretGenerator.generate();

        seedRepository.upsert(appId, resolvedIssuer, secret);

        QrData qrData = qrDataFactory.newBuilder().label(appId).secret(secret).issuer(resolvedIssuer).build();
        log.info("TOTP | app registered | appId={} issuer={}", appId, resolvedIssuer);

        return new RegisterDeviceResponse(appId, resolvedIssuer, secret, qrData.getUri());
    }

    /** Renders the enrollment QR code for the secret already persisted against {@code appId}. */
    public GenerateQrResponse generateQr(String appId) {
        TotpSeed seed = requireSeed(appId);

        QrData qrData = qrDataFactory.newBuilder().label(appId).secret(seed.secret()).issuer(seed.issuer()).build();
        String qrCodeDataUri = renderQrDataUri(qrData);

        return new GenerateQrResponse(appId, seed.issuer(), qrData.getUri(), qrCodeDataUri);
    }

    /** Computes the current numeric code for the secret already persisted against {@code appId}. */
    public GenerateOtpResponse generateOtp(String appId) {
        TotpSeed seed = requireSeed(appId);

        long now = timeProvider.getTime();
        try {
            String code = codeGenerator.generate(seed.secret(), now / timePeriod);
            int validForSeconds = timePeriod - (int) (now % timePeriod);
            return new GenerateOtpResponse(appId, code, validForSeconds);
        } catch (CodeGenerationException e) {
            throw new OtpGenerationException("Failed to generate the OTP code for appId: " + appId, e);
        }
    }

    /**
     * Looks up the persisted seed for {@code appId} and checks the code against it for the
     * current time step (+/- configured discrepancy).
     */
    public boolean validate(String appId, String code) {
        TotpSeed seed = requireSeed(appId);
        boolean valid = codeVerifier.isValidCode(seed.secret(), code);
        log.info("TOTP | validate | appId={} valid={}", appId, valid);
        return valid;
    }

    private TotpSeed requireSeed(String appId) {
        TotpSeed seed = seedRepository.findByAppId(appId);
        if (seed == null) {
            throw new AppNotFoundException(appId);
        }
        return seed;
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
