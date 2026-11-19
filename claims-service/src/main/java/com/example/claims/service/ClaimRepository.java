package com.example.claims.service;

import com.example.claims.domain.Claim;
import com.example.claims.domain.ClaimStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    List<Claim> findByClaimantIdOrderByCreatedAtDesc(String claimantId);
    List<Claim> findByStatusOrderByCreatedAtAsc(ClaimStatus status);
    List<Claim> findByAssignedOfficerIdOrderByUpdatedAtDesc(String officerId);
    List<Claim> findByAssignedOfficerIdIsNotNull();
}
