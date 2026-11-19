package com.example.claims.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The claim lifecycle. All legal transitions live here so the rules are in one
 * place and trivially unit-testable.
 *
 * SUBMITTED -> ASSIGNED -> UNDER_REVIEW <-> INFO_REQUESTED
 *                              |-> APPROVED -> SETTLED
 *                              |-> REJECTED
 */
public enum ClaimStatus {
    SUBMITTED, ASSIGNED, UNDER_REVIEW, INFO_REQUESTED, APPROVED, REJECTED, SETTLED;

    private static final Map<ClaimStatus, Set<ClaimStatus>> ALLOWED = Map.of(
            SUBMITTED, EnumSet.of(ASSIGNED),
            ASSIGNED, EnumSet.of(UNDER_REVIEW),
            UNDER_REVIEW, EnumSet.of(INFO_REQUESTED, APPROVED, REJECTED),
            INFO_REQUESTED, EnumSet.of(UNDER_REVIEW),
            APPROVED, EnumSet.of(SETTLED),
            REJECTED, EnumSet.noneOf(ClaimStatus.class),
            SETTLED, EnumSet.noneOf(ClaimStatus.class));

    public boolean canMoveTo(ClaimStatus next) {
        return ALLOWED.get(this).contains(next);
    }

    /** Terminal states carry no outstanding liability and need no further work. */
    public boolean isTerminal() {
        return this == REJECTED || this == SETTLED;
    }
}
