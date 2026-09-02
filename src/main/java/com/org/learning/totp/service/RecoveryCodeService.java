package com.org.learning.totp.service;

import com.org.learning.totp.exception.AppNotFoundException;
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
 * RecoveryCodeGenerator} bean. A code is a fallback path into an app already enrolled for TOTP
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

    /** Generates a fresh batch of codes for {@code appId}, replacing any previously issued batch. */
    public List<String> generate(String appId, Integer count) {
        if (seedRepository.findByAppId(appId) == null) {
            throw new AppNotFoundException(appId);
        }

        int resolvedCount = count == null ? DEFAULT_COUNT : count;
        List<String> codes = Arrays.asList(recoveryCodeGenerator.generateCodes(resolvedCount));
        recoveryCodeRepository.replaceAll(appId, codes);
        log.info("TOTP | recovery codes generated | appId={} count={}", appId, resolvedCount);

        return codes;
    }

    /** Redeems a code — {@code true} only the first time this exact unused code is submitted. */
    public boolean validate(String appId, String code) {
        boolean valid = recoveryCodeRepository.redeem(appId, code);
        log.info("TOTP | recovery code validate | appId={} valid={}", appId, valid);
        return valid;
    }
}
