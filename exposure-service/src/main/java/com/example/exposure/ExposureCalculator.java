package com.example.exposure;

import java.math.BigDecimal;

/**
 * Outstanding liability for one claim:
 *  - REJECTED / SETTLED -> 0 (nothing left to pay)
 *  - APPROVED           -> the approved amount (a firm obligation not yet paid)
 *  - everything else    -> the claimant's estimate (our reserve while we assess)
 */
public final class ExposureCalculator {
    private ExposureCalculator() { }

    public static boolean isOpen(String status) {
        return !"REJECTED".equals(status) && !"SETTLED".equals(status);
    }

    public static BigDecimal outstanding(String status, BigDecimal estimated, BigDecimal approved) {
        if (!isOpen(status)) return BigDecimal.ZERO;
        if ("APPROVED".equals(status) && approved != null) return approved;
        return estimated == null ? BigDecimal.ZERO : estimated;
    }
}
