package com.example.claims.events;

import com.example.claims.domain.Claim;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The integration contract published to Kafka (topic claims.events, keyed by claimId so
 * all events for one claim land on one partition, in order). It carries a snapshot of
 * the fields downstream consumers need, plus {@code revision} so they can de-duplicate.
 */
public record ClaimEvent(
        UUID eventId,
        String eventType,
        UUID claimId,
        String claimNumber,
        String market,
        String claimType,
        String status,
        BigDecimal estimatedAmount,
        BigDecimal approvedAmount,
        int revision,
        Instant occurredAt) {

    public static ClaimEvent of(String eventType, Claim c) {
        return new ClaimEvent(UUID.randomUUID(), eventType, c.getId(), c.getClaimNumber(),
                c.getMarket().name(), c.getType().name(), c.getStatus().name(),
                c.getEstimatedAmount(), c.getApprovedAmount(), c.getRevision(), Instant.now());
    }
}
