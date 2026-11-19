package com.example.claims.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Append-only audit trail: who did what to a claim, and when. */
@Entity
@Table(name = "claim_audit", indexes = @Index(name = "idx_audit_claim", columnList = "claimId"))
public class ClaimAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private UUID claimId;
    @Column(nullable = false)
    private String action;
    @Column(nullable = false)
    private String actor;
    @Column(length = 1000)
    private String detail;
    @Column(nullable = false)
    private Instant occurredAt = Instant.now();

    protected ClaimAudit() { }

    public ClaimAudit(UUID claimId, String action, String actor, String detail) {
        this.claimId = claimId;
        this.action = action;
        this.actor = actor;
        this.detail = detail;
    }

    public UUID getClaimId() { return claimId; }
    public String getAction() { return action; }
    public String getActor() { return actor; }
    public String getDetail() { return detail; }
    public Instant getOccurredAt() { return occurredAt; }
}
