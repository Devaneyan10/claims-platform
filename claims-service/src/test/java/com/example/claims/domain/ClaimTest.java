package com.example.claims.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ClaimTest {

    private Claim newClaim() {
        return new Claim("CLM-SG-TEST", "alice", "Alice Tan", Market.SG, ClaimType.MOTOR,
                "Rear-ended at a junction", LocalDate.now().minusDays(1), new BigDecimal("4000.00"));
    }

    @Test
    void happyPathToSettlement() {
        Claim c = newClaim();
        c.assign("officer1");
        c.startReview();
        c.approve(new BigDecimal("3500.00"), "Covered under policy");
        c.settle();
        assertEquals(ClaimStatus.SETTLED, c.getStatus());
        assertEquals("officer1", c.getAssignedOfficerId());
        assertEquals(new BigDecimal("3500.00"), c.getApprovedAmount());
        assertNotNull(c.getDecidedAt());
    }

    @Test
    void infoRequestLoopReturnsToReview() {
        Claim c = newClaim();
        c.assign("officer1");
        c.startReview();
        c.requestInfo();
        assertEquals(ClaimStatus.INFO_REQUESTED, c.getStatus());
        c.infoProvided();
        assertEquals(ClaimStatus.UNDER_REVIEW, c.getStatus());
    }

    @Test
    void cannotDecideBeforeReview() {
        Claim c = newClaim();
        assertThrows(InvalidTransitionException.class, () -> c.reject("nope"));
        c.assign("officer1");
        assertThrows(InvalidTransitionException.class, () -> c.approve(BigDecimal.TEN, "x"));
    }

    @Test
    void cannotSettleRejectedClaim() {
        Claim c = newClaim();
        c.assign("officer1");
        c.startReview();
        c.reject("Not covered");
        assertThrows(InvalidTransitionException.class, c::settle);
    }

    @Test
    void approvalNeedsPositiveAmountAndLeavesStateUntouched() {
        Claim c = newClaim();
        c.assign("officer1");
        c.startReview();
        assertThrows(IllegalArgumentException.class, () -> c.approve(BigDecimal.ZERO, "x"));
        assertEquals(ClaimStatus.UNDER_REVIEW, c.getStatus());
    }

    @Test
    void everyTransitionBumpsRevision() {
        Claim c = newClaim();
        int before = c.getRevision();
        c.assign("officer1");
        assertEquals(before + 1, c.getRevision());
    }

    @Test
    void terminalStatesHaveNoOutgoingTransitions() {
        for (ClaimStatus from : ClaimStatus.values()) {
            if (!from.isTerminal()) continue;
            for (ClaimStatus to : ClaimStatus.values()) assertFalse(from.canMoveTo(to));
        }
    }
}
