package com.example.claims.service;

import com.example.claims.domain.*;
import com.example.claims.events.ClaimEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates claim use-cases. Every state change: (1) goes through the Claim aggregate,
 * (2) writes an audit row, (3) publishes a ClaimEvent that is sent to Kafka after commit.
 */
@Service
@Transactional
public class ClaimService {
    private final ClaimRepository claims;
    private final InfoRequestRepository infoRequests;
    private final ClaimAuditRepository audit;
    private final ApplicationEventPublisher events;

    public ClaimService(ClaimRepository claims, InfoRequestRepository infoRequests,
                        ClaimAuditRepository audit, ApplicationEventPublisher events) {
        this.claims = claims;
        this.infoRequests = infoRequests;
        this.audit = audit;
        this.events = events;
    }

    // ---------- claimant use-cases ----------

    public Claim submit(Caller caller, SubmitClaim cmd) {
        caller.requireRole(Role.CLAIMANT);
        String number = "CLM-" + cmd.market() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Claim claim = claims.save(new Claim(number, caller.userId(), cmd.claimantName(), cmd.market(),
                cmd.type(), cmd.description(), cmd.incidentDate(), cmd.estimatedAmount()));
        changed(claim, "SUBMITTED", caller, null);
        return claim;
    }

    public InfoRequest answerInfoRequest(Caller caller, UUID claimId, UUID infoRequestId, String answer) {
        caller.requireRole(Role.CLAIMANT);
        Claim claim = find(claimId);
        if (!claim.getClaimantId().equals(caller.userId())) throw new ForbiddenException("Not your claim");
        InfoRequest request = infoRequests.findById(infoRequestId)
                .filter(r -> r.getClaimId().equals(claimId))
                .orElseThrow(() -> new NotFoundException("Info request not found"));
        request.answer(answer);
        infoRequests.save(request);
        audit(claim, "INFO_ANSWERED", caller, request.getQuestion());
        // Once every outstanding question is answered the claim goes back to the officer.
        if (!infoRequests.existsByClaimIdAndAnswerIsNull(claimId)) {
            claim.infoProvided();
            changed(claim, "INFO_PROVIDED", caller, null);
        }
        return request;
    }

    // ---------- officer use-cases ----------

    public Claim assign(Caller caller, UUID claimId) {
        caller.requireRole(Role.OFFICER);
        Claim claim = find(claimId);
        claim.assign(caller.userId());          // @Version stops two officers winning the same claim
        changed(claim, "ASSIGNED", caller, null);
        return claim;
    }

    public Claim startReview(Caller caller, UUID claimId) {
        Claim claim = officerClaim(caller, claimId);
        claim.startReview();
        changed(claim, "REVIEW_STARTED", caller, null);
        return claim;
    }

    public InfoRequest requestInfo(Caller caller, UUID claimId, String question) {
        Claim claim = officerClaim(caller, claimId);
        claim.requestInfo();
        InfoRequest request = infoRequests.save(new InfoRequest(claimId, question, caller.userId()));
        changed(claim, "INFO_REQUESTED", caller, question);
        return request;
    }

    public Claim decide(Caller caller, UUID claimId, boolean approved, BigDecimal amount, String reason) {
        Claim claim = officerClaim(caller, claimId);
        if (approved) claim.approve(amount, reason); else claim.reject(reason);
        changed(claim, approved ? "APPROVED" : "REJECTED", caller, reason);
        return claim;
    }

    public Claim settle(Caller caller, UUID claimId) {
        Claim claim = officerClaim(caller, claimId);
        claim.settle();
        changed(claim, "SETTLED", caller, null);
        return claim;
    }

    // ---------- queries ----------

    @Transactional(readOnly = true)
    public Claim get(Caller caller, UUID claimId) {
        Claim claim = find(claimId);
        if (!caller.isStaff() && !claim.getClaimantId().equals(caller.userId())) {
            throw new ForbiddenException("Not your claim");
        }
        return claim;
    }

    @Transactional(readOnly = true)
    public List<Claim> mine(Caller caller) {
        caller.requireRole(Role.CLAIMANT);
        return claims.findByClaimantIdOrderByCreatedAtDesc(caller.userId());
    }

    /** Unassigned claims waiting for an officer, oldest first. */
    @Transactional(readOnly = true)
    public List<Claim> queue(Caller caller) {
        caller.requireStaff();
        return claims.findByStatusOrderByCreatedAtAsc(ClaimStatus.SUBMITTED);
    }

    @Transactional(readOnly = true)
    public List<Claim> assignedTo(Caller caller) {
        caller.requireRole(Role.OFFICER);
        return claims.findByAssignedOfficerIdOrderByUpdatedAtDesc(caller.userId());
    }

    @Transactional(readOnly = true)
    public List<Claim> all(Caller caller, ClaimStatus status) {
        caller.requireStaff();
        return status == null ? claims.findAll() : claims.findByStatusOrderByCreatedAtAsc(status);
    }

    @Transactional(readOnly = true)
    public List<InfoRequest> infoRequests(Caller caller, UUID claimId) {
        get(caller, claimId); // authorization check
        return infoRequests.findByClaimIdOrderByRequestedAtAsc(claimId);
    }

    @Transactional(readOnly = true)
    public List<ClaimAudit> auditTrail(Caller caller, UUID claimId) {
        get(caller, claimId);
        return audit.findByClaimIdOrderByOccurredAtAsc(claimId);
    }

    // ---------- helpers ----------

    private Claim find(UUID id) {
        return claims.findById(id).orElseThrow(() -> new NotFoundException("Claim not found: " + id));
    }

    private Claim officerClaim(Caller caller, UUID claimId) {
        caller.requireRole(Role.OFFICER);
        Claim claim = find(claimId);
        if (!caller.userId().equals(claim.getAssignedOfficerId())) {
            throw new ForbiddenException("This claim is not assigned to you");
        }
        return claim;
    }

    private void audit(Claim claim, String action, Caller caller, String detail) {
        audit.save(new ClaimAudit(claim.getId(), action, caller.userId(), detail));
    }

    private void changed(Claim claim, String action, Caller caller, String detail) {
        audit(claim, action, caller, detail);
        events.publishEvent(ClaimEvent.of(action, claim));
    }
}
