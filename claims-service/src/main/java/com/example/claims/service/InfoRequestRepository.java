package com.example.claims.service;

import com.example.claims.domain.InfoRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InfoRequestRepository extends JpaRepository<InfoRequest, UUID> {
    List<InfoRequest> findByClaimIdOrderByRequestedAtAsc(UUID claimId);
    boolean existsByClaimIdAndAnswerIsNull(UUID claimId);
}
