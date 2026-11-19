package com.example.exposure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExposureServiceTest {
    private final ExposureRepository repo = mock(ExposureRepository.class);
    private final ExposureService service = new ExposureService(repo);
    private final UUID claimId = UUID.randomUUID();

    private ClaimEvent event(String status, int revision, String approved) {
        return new ClaimEvent(UUID.randomUUID(), "X", claimId, "CLM-SG-1", "SG", "MOTOR", status,
                new BigDecimal("4000.00"), approved == null ? null : new BigDecimal(approved), revision, Instant.now());
    }

    @Test
    void firstEventCreatesRow() {
        when(repo.findById(claimId)).thenReturn(Optional.empty());
        assertTrue(service.apply(event("SUBMITTED", 1, null)));
        verify(repo).save(any(ClaimExposure.class));
    }

    @Test
    void duplicateOrStaleEventIsIgnored() {
        ClaimExposure existing = new ClaimExposure(claimId);
        existing.apply(event("UNDER_REVIEW", 3, null), new BigDecimal("4000.00"));
        when(repo.findById(claimId)).thenReturn(Optional.of(existing));

        assertFalse(service.apply(event("UNDER_REVIEW", 3, null)), "redelivery of same revision");
        assertFalse(service.apply(event("ASSIGNED", 2, null)), "out-of-date revision");
        verify(repo, never()).save(any());
    }

    @Test
    void newerEventUpdatesExposure() {
        ClaimExposure existing = new ClaimExposure(claimId);
        existing.apply(event("UNDER_REVIEW", 3, null), new BigDecimal("4000.00"));
        when(repo.findById(claimId)).thenReturn(Optional.of(existing));

        assertTrue(service.apply(event("APPROVED", 4, "3500.00")));
        assertEquals(new BigDecimal("3500.00"), existing.getExposureAmount());
    }
}
