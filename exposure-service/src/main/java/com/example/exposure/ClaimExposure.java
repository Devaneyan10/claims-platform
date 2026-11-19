package com.example.exposure;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Read model: one row per claim holding its current outstanding exposure. */
@Entity
@Table(name = "claim_exposure", indexes = @Index(name = "idx_exposure_market", columnList = "market"))
public class ClaimExposure {
    @Id
    private UUID claimId;
    private String claimNumber;
    private String market;
    private String status;
    @Column(precision = 15, scale = 2)
    private BigDecimal exposureAmount;
    private int revision;
    private Instant updatedAt;

    protected ClaimExposure() { }

    public ClaimExposure(UUID claimId) { this.claimId = claimId; }

    public void apply(ClaimEvent e, BigDecimal exposure) {
        this.claimNumber = e.claimNumber();
        this.market = e.market();
        this.status = e.status();
        this.exposureAmount = exposure;
        this.revision = e.revision();
        this.updatedAt = Instant.now();
    }

    public UUID getClaimId() { return claimId; }
    public String getStatus() { return status; }
    public String getMarket() { return market; }
    public BigDecimal getExposureAmount() { return exposureAmount; }
    public int getRevision() { return revision; }
}
