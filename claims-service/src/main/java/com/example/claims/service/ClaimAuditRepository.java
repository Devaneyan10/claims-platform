package com.example.claims.service;

import com.example.claims.domain.ClaimAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimAuditRepository extends JpaRepository<ClaimAudit, Long> {
    List<ClaimAudit> findByClaimIdOrderByOccurredAtAsc(UUID claimId);
}
