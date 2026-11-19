package com.example.exposure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * This service's own copy of the claims.events contract. Deliberately NOT a shared library:
 * consumers own their view of the contract and ignore fields they do not need, so services
 * can be deployed independently. Status and market stay as strings for the same reason.
 */
public record ClaimEvent(UUID eventId, String eventType, UUID claimId, String claimNumber, String market,
                         String claimType, String status, BigDecimal estimatedAmount, BigDecimal approvedAmount,
                         int revision, Instant occurredAt) { }
