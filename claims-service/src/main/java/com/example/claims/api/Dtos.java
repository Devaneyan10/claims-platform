package com.example.claims.api;

import com.example.claims.domain.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** API contract. Entities are never returned directly, so the DB model can change freely. */
public final class Dtos {
    private Dtos() { }

    public record ClaimResponse(UUID id, String claimNumber, String claimantName, Market market, ClaimType type,
                                String description, LocalDate incidentDate, BigDecimal estimatedAmount,
                                BigDecimal approvedAmount, String decisionReason, ClaimStatus status,
                                String assignedOfficerId, Instant createdAt, Instant updatedAt) {
        public static ClaimResponse from(Claim c) {
            return new ClaimResponse(c.getId(), c.getClaimNumber(), c.getClaimantName(), c.getMarket(), c.getType(),
                    c.getDescription(), c.getIncidentDate(), c.getEstimatedAmount(), c.getApprovedAmount(),
                    c.getDecisionReason(), c.getStatus(), c.getAssignedOfficerId(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }

    public record InfoRequestResponse(UUID id, String question, String answer, String requestedBy,
                                      Instant requestedAt, Instant answeredAt) {
        public static InfoRequestResponse from(InfoRequest r) {
            return new InfoRequestResponse(r.getId(), r.getQuestion(), r.getAnswer(), r.getRequestedBy(),
                    r.getRequestedAt(), r.getAnsweredAt());
        }
    }

    public record AuditEntry(String action, String actor, String detail, Instant occurredAt) {
        public static AuditEntry from(ClaimAudit a) {
            return new AuditEntry(a.getAction(), a.getActor(), a.getDetail(), a.getOccurredAt());
        }
    }

    public record InfoRequestBody(@NotBlank @Size(max = 1000) String question) { }

    public record InfoAnswerBody(@NotBlank @Size(max = 2000) String answer) { }

    public record DecisionBody(@NotNull Boolean approved, BigDecimal approvedAmount,
                               @NotBlank @Size(max = 1000) String reason) { }
}
