package com.example.exposure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExposureService {
    private static final Logger log = LoggerFactory.getLogger(ExposureService.class);

    public record MarketExposure(String market, long openClaims, BigDecimal outstandingExposure) { }
    public record StatusCount(String status, long count) { }
    public record ExposureSummary(BigDecimal totalOutstandingExposure, long openClaims,
                                  List<MarketExposure> byMarket, List<StatusCount> byStatus, Instant asOf) { }

    private final ExposureRepository repo;

    public ExposureService(ExposureRepository repo) { this.repo = repo; }

    @KafkaListener(topics = "${app.kafka.claims-topic}")
    public void onEvent(ClaimEvent event) {
        boolean applied = apply(event);
        log.info("{} {} rev {} -> {}", event.eventType(), event.claimNumber(), event.revision(),
                applied ? "applied" : "ignored (stale or duplicate)");
    }

    /**
     * Idempotent: Kafka guarantees at-least-once delivery, so duplicates and redeliveries
     * are expected. An event only changes state if its revision is newer than what we hold.
     */
    @Transactional
    public boolean apply(ClaimEvent event) {
        ClaimExposure row = repo.findById(event.claimId()).orElseGet(() -> new ClaimExposure(event.claimId()));
        if (row.getRevision() >= event.revision()) return false;
        row.apply(event, ExposureCalculator.outstanding(event.status(), event.estimatedAmount(), event.approvedAmount()));
        repo.save(row);
        return true;
    }

    @Transactional(readOnly = true)
    public ExposureSummary summary() {
        List<MarketExposure> byMarket = repo.openByMarket().stream()
                .map(r -> new MarketExposure((String) r[0], ((Number) r[1]).longValue(),
                        r[2] == null ? BigDecimal.ZERO : (BigDecimal) r[2]))
                .toList();
        List<StatusCount> byStatus = repo.countByStatus().stream()
                .map(r -> new StatusCount((String) r[0], ((Number) r[1]).longValue()))
                .toList();
        BigDecimal total = byMarket.stream().map(MarketExposure::outstandingExposure)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long open = byMarket.stream().mapToLong(MarketExposure::openClaims).sum();
        return new ExposureSummary(total, open, byMarket, byStatus, Instant.now());
    }
}
