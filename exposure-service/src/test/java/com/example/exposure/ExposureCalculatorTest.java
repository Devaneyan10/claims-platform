package com.example.exposure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ExposureCalculatorTest {
    private static final BigDecimal EST = new BigDecimal("4000.00");
    private static final BigDecimal APPROVED = new BigDecimal("3500.00");

    @Test
    void openClaimsReserveTheEstimate() {
        for (String s : new String[]{"SUBMITTED", "ASSIGNED", "UNDER_REVIEW", "INFO_REQUESTED"}) {
            assertEquals(EST, ExposureCalculator.outstanding(s, EST, null), s);
        }
    }

    @Test
    void approvedClaimsUseApprovedAmount() {
        assertEquals(APPROVED, ExposureCalculator.outstanding("APPROVED", EST, APPROVED));
    }

    @Test
    void rejectedAndSettledClaimsCarryNoExposure() {
        assertEquals(BigDecimal.ZERO, ExposureCalculator.outstanding("REJECTED", EST, null));
        assertEquals(BigDecimal.ZERO, ExposureCalculator.outstanding("SETTLED", EST, APPROVED));
    }
}
