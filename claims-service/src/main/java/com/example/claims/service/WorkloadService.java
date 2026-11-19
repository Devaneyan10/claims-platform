package com.example.claims.service;

import com.example.claims.domain.Claim;
import com.example.claims.domain.ClaimStatus;
import com.example.claims.domain.Role;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workload and performance per officer. Officers see only themselves; managers see the team.
 * Computed in memory from assigned claims, which is fine at this scale; at real volume this
 * would be a SQL aggregate or a read model.
 */
@Service
@Transactional(readOnly = true)
public class WorkloadService {

    public record OfficerWorkload(String officerId, long openClaims, long decidedClaims,
                                  Double avgHoursToDecision, Map<ClaimStatus, Long> byStatus) { }

    private final ClaimRepository claims;

    public WorkloadService(ClaimRepository claims) { this.claims = claims; }

    public List<OfficerWorkload> workload(Caller caller) {
        caller.requireStaff();
        Map<String, List<Claim>> byOfficer = claims.findByAssignedOfficerIdIsNotNull().stream()
                .filter(c -> caller.role() == Role.MANAGER || caller.userId().equals(c.getAssignedOfficerId()))
                .collect(Collectors.groupingBy(Claim::getAssignedOfficerId, TreeMap::new, Collectors.toList()));

        return byOfficer.entrySet().stream().map(e -> {
            List<Claim> list = e.getValue();
            Map<ClaimStatus, Long> byStatus = list.stream()
                    .collect(Collectors.groupingBy(Claim::getStatus, () -> new EnumMap<>(ClaimStatus.class), Collectors.counting()));
            long open = list.stream().filter(c -> !c.getStatus().isTerminal()).count();
            List<Claim> decided = list.stream().filter(c -> c.getDecidedAt() != null).toList();
            Double avgHours = decided.isEmpty() ? null : decided.stream()
                    .mapToDouble(c -> Duration.between(c.getCreatedAt(), c.getDecidedAt()).toMinutes() / 60.0)
                    .average().orElse(0);
            return new OfficerWorkload(e.getKey(), open, decided.size(), avgHours, byStatus);
        }).toList();
    }
}
