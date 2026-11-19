package com.example.claims.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root. State changes only happen through the methods below, which
 * enforce the lifecycle and bump {@code revision} (used by consumers to ignore
 * stale or duplicate events).
 */
@Entity
@Table(name = "claims", indexes = {
        @Index(name = "idx_claim_status", columnList = "status"),
        @Index(name = "idx_claim_officer", columnList = "assignedOfficerId"),
        @Index(name = "idx_claim_claimant", columnList = "claimantId")})
public class Claim {

    @Id
    private UUID id = UUID.randomUUID();
    @Column(unique = true, nullable = false)
    private String claimNumber;
    @Column(nullable = false)
    private String claimantId;
    @Column(nullable = false)
    private String claimantName;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Market market;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ClaimType type;
    @Column(nullable = false, length = 2000)
    private String description;
    @Column(nullable = false)
    private LocalDate incidentDate;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal estimatedAmount;
    @Column(precision = 15, scale = 2)
    private BigDecimal approvedAmount;
    @Column(length = 1000)
    private String decisionReason;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ClaimStatus status = ClaimStatus.SUBMITTED;
    private String assignedOfficerId;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    private Instant decidedAt;
    /** Business revision, bumped on every transition; carried on events. */
    private int revision = 1;
    /** JPA optimistic lock: stops two officers claiming the same claim. */
    @Version
    private long version;

    protected Claim() { }

    public Claim(String claimNumber, String claimantId, String claimantName, Market market,
                 ClaimType type, String description, LocalDate incidentDate, BigDecimal estimatedAmount) {
        this.claimNumber = claimNumber;
        this.claimantId = claimantId;
        this.claimantName = claimantName;
        this.market = market;
        this.type = type;
        this.description = description;
        this.incidentDate = incidentDate;
        this.estimatedAmount = estimatedAmount;
    }

    public void assign(String officerId) { moveTo(ClaimStatus.ASSIGNED); this.assignedOfficerId = officerId; }
    public void startReview() { moveTo(ClaimStatus.UNDER_REVIEW); }
    public void requestInfo() { moveTo(ClaimStatus.INFO_REQUESTED); }
    public void infoProvided() { moveTo(ClaimStatus.UNDER_REVIEW); }

    public void approve(BigDecimal amount, String reason) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("An approved claim needs a positive approved amount");
        }
        moveTo(ClaimStatus.APPROVED);
        this.approvedAmount = amount;
        this.decisionReason = reason;
        this.decidedAt = Instant.now();
    }

    public void reject(String reason) {
        moveTo(ClaimStatus.REJECTED);
        this.decisionReason = reason;
        this.decidedAt = Instant.now();
    }

    public void settle() { moveTo(ClaimStatus.SETTLED); }

    private void moveTo(ClaimStatus next) {
        if (!status.canMoveTo(next)) {
            throw new InvalidTransitionException(status, next);
        }
        status = next;
        revision++;
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getClaimNumber() { return claimNumber; }
    public String getClaimantId() { return claimantId; }
    public String getClaimantName() { return claimantName; }
    public Market getMarket() { return market; }
    public ClaimType getType() { return type; }
    public String getDescription() { return description; }
    public LocalDate getIncidentDate() { return incidentDate; }
    public BigDecimal getEstimatedAmount() { return estimatedAmount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public String getDecisionReason() { return decisionReason; }
    public ClaimStatus getStatus() { return status; }
    public String getAssignedOfficerId() { return assignedOfficerId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public int getRevision() { return revision; }
}
