package com.org.learning.totp.service;

import com.org.learning.totp.exception.DeviceNotFoundException;
import com.org.learning.totp.repository.RecoveryCodeRepository;
import com.org.learning.totp.repository.TotpSeedRepository;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Issues and redeems one-time backup codes via the starter's auto-configured {@link
 * RecoveryCodeGenerator} bean. A code is a fallback path into a device already enrolled for TOTP
 * (see {@link TotpService}) — generating a batch requires an existing {@code totp_seed} row.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecoveryCodeService {

    private static final int DEFAULT_COUNT = 10;

    private final RecoveryCodeGenerator recoveryCodeGenerator;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final TotpSeedRepository seedRepository;

    /** Generates a fresh batch of codes for {@code deviceId}, replacing any previously issued batch. */
    public List<String> generate(String deviceId, Integer count) {
        if (seedRepository.findByDeviceId(deviceId) == null) {
            throw new DeviceNotFoundException(deviceId);
        }

        int resolvedCount = count == null ? DEFAULT_COUNT : count;
        List<String> codes = Arrays.asList(recoveryCodeGenerator.generateCodes(resolvedCount));
        recoveryCodeRepository.replaceAll(deviceId, codes);
        log.info("TOTP | recovery codes generated | deviceId={} count={}", deviceId, resolvedCount);

        return codes;
    }

    /** Redeems a code — {@code true} only the first time this exact unused code is submitted. */
    public boolean validate(String deviceId, String code) {
        boolean valid = recoveryCodeRepository.redeem(deviceId, code);
        log.info("TOTP | recovery code validate | deviceId={} valid={}", deviceId, valid);
        return valid;
    }
}
