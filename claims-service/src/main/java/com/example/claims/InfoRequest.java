package com.example.claims.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** A question an officer asks the claimant while the claim is INFO_REQUESTED. */
@Entity
@Table(name = "info_requests", indexes = @Index(name = "idx_info_claim", columnList = "claimId"))
public class InfoRequest {
    @Id
    private UUID id = UUID.randomUUID();
    @Column(nullable = false)
    private UUID claimId;
    @Column(nullable = false, length = 1000)
    private String question;
    @Column(length = 2000)
    private String answer;
    @Column(nullable = false)
    private String requestedBy;
    @Column(nullable = false)
    private Instant requestedAt = Instant.now();
    private Instant answeredAt;

    protected InfoRequest() { }

    public InfoRequest(UUID claimId, String question, String requestedBy) {
        this.claimId = claimId;
        this.question = question;
        this.requestedBy = requestedBy;
    }

    public void answer(String answer) {
        if (this.answer != null) throw new IllegalStateException("This request has already been answered");
        this.answer = answer;
        this.answeredAt = Instant.now();
    }

    public boolean isAnswered() { return answer != null; }
    public UUID getId() { return id; }
    public UUID getClaimId() { return claimId; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getAnsweredAt() { return answeredAt; }
}
