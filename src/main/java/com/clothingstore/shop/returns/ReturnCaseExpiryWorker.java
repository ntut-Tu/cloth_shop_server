package com.clothingstore.shop.returns;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class ReturnCaseExpiryWorker {
    private final ReturnCaseRepository repository;

    public ReturnCaseExpiryWorker(ReturnCaseRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedDelayString = "${return-cases.sweep-delay-ms:60000}")
    @Transactional
    public void sweep() {
        for (ReturnCaseRepository.Case entry : repository.lockExpiredBatch(100)) {
            boolean vendorTimeout = entry.status() == ReturnCaseState.VENDOR_PENDING;
            ReturnCaseState next = vendorTimeout ? ReturnCaseState.ADMIN_PENDING : ReturnCaseState.EXPIRED;
            String type = vendorTimeout ? "VENDOR_TIMEOUT" : "APPEAL_EXPIRED";
            String note = vendorTimeout ? "Vendor response deadline elapsed" : "Customer appeal deadline elapsed";
            if (repository.transition(entry.id(), entry.status(), next, null, null, null, !vendorTimeout) != 1) {
                throw new IllegalStateException("Return case changed during expiry sweep");
            }
            if (!repository.appendAction(entry.id(), null, "system", "system-" + type + "-" + entry.id(),
                    type, entry.status(), next, digest(entry.id() + "|" + type), note)) {
                throw new IllegalStateException("Return case expiry audit could not be saved");
            }
        }
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
