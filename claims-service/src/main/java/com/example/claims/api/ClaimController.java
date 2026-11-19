package com.example.claims.api;

import com.example.claims.api.Dtos.*;
import com.example.claims.domain.ClaimStatus;
import com.example.claims.domain.Role;
import com.example.claims.service.Caller;
import com.example.claims.service.ClaimService;
import com.example.claims.service.SubmitClaim;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Identity is passed in X-User-Id and X-User-Role headers (CLAIMANT | OFFICER | MANAGER).
 * Authorization is enforced in the service layer, not here.
 */
@RestController
@RequestMapping("/api/claims")
public class ClaimController {
    private final ClaimService service;

    public ClaimController(ClaimService service) { this.service = service; }

    private static Caller caller(String userId, Role role) { return new Caller(userId, role); }

    // ----- claimant -----
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimResponse submit(@RequestHeader("X-User-Id") String u, @RequestHeader("X-User-Role") Role r,
                                @Valid @RequestBody SubmitClaim body) {
        return ClaimResponse.from(service.submit(caller(u, r), body));
    }

    @GetMapping("/mine")
    public List<ClaimResponse> mine(@RequestHeader("X-User-Id") String u, @RequestHeader("X-User-Role") Role r) {
        return service.mine(caller(u, r)).stream().map(ClaimResponse::from).toList();
    }

    @PostMapping("/{id}/info-requests/{requestId}/answer")
    public InfoRequestResponse answer(@RequestHeader("X-User-Id") String u, @RequestHeader("X-User-Role") Role r,
                                      @PathVariable UUID id, @PathVariable UUID requestId,
                                      @Valid @RequestBody InfoAnswerBody body) {
        return InfoRequestResponse.from(service.answerInfoRequest(caller(u, r), id, requestId, body.answer()));
    }

    // ----- staff -----
    @GetMapping("/queue")
    public List<ClaimResponse> queue(@RequestHeader("X-User-Id") String u, @RequestHeader("X-User-Role") Role r) {
        return service.queue(caller(u, r)).stream().map(ClaimResponse::from).toList();
    }

    @GetMapping("/assigned")
    public List<ClaimResponse> assigned(@RequestHeader("X-User-Id") String u, @RequestHeader("X-User-Role") Role r) {
        return service.assignedTo(caller(u, r)).stream().map(ClaimResponse::from).toList();
    }

    @GetMapping
    public List<ClaimResponse> all(@RequestHeader("X-User-Id") String u, @RequestHeader("X-User-Role") Role r,
                                   @RequestParam(required = false) ClaimStatus status) {
        return service.all(caller(u, r), status).stream().map(ClaimResponse::from).toList();
    }

    @PostMapping("/{id}/assign")
    public ClaimResponse assign(@RequestHeader("X-User-Id") String u, @RequestHeader("X-User-Role") Role r,
                                @PathVariable UUID id) {
        return ClaimResponse.from(service.assign(caller(u, r), id));
    }

    @PostMapping("/{id}/start-review")
    public ClaimResponse startReview(@RequestHeader("X-User-Id") String u, @RequestHeader("X-User-Role") Role r,
                                     @PathVariable UUID id) {
        return ClaimResponse.from(service.startReview(caller(u, r), id));
    }

    @PostMapping("/{id}/info-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public InfoRequestResponse requestInfo(@RequestHeader("X-User-Id") String u, @RequestHeader("X-User-Role") Role r,
                                           @PathVariable UUID id, @Valid @RequestBody InfoRequestBody body) {
        return InfoRequestResponse.from(service.requestInfo(caller(u, r), id, body.question()));
    }

    @PostMapping("/{id}/decision")
    public ClaimResponse decide(@RequestHeader("X-User-Id") String u, @RequestHeader("X-User-Role") Role r,
                                @PathVariable UUID id, @Valid @RequestBody DecisionBody body) {
        return ClaimResponse.from(service.decide(caller(u, r), id, body.approved(), body.approvedAmount(), body.reason()));
    }

    @PostMapping("/{id}/settle")
    public ClaimResponse settle(@RequestHeader("X-User-Id") String u, @RequestHeader("X-User-Role") Role r,
                                @PathVariable UUID id) {
        return ClaimResponse.from(service.settle(caller(u, r), id));
    }

    // ----- shared reads -----
    @GetMapping("/{id}")
    public ClaimResponse get(@RequestHeader("X-User-Id") String u, @RequestHeader("X-User-Role") Role r,
                             @PathVariable UUID id) {
        return ClaimResponse.from(service.get(caller(u, r), id));
    }

    @GetMapping("/{id}/info-requests")
    public List<InfoRequestResponse> infoRequests(@RequestHeader("X-User-Id") String u,
                                                  @RequestHeader("X-User-Role") Role r, @PathVariable UUID id) {
        return service.infoRequests(caller(u, r), id).stream().map(InfoRequestResponse::from).toList();
    }

    @GetMapping("/{id}/audit")
    public List<AuditEntry> audit(@RequestHeader("X-User-Id") String u, @RequestHeader("X-User-Role") Role r,
                                  @PathVariable UUID id) {
        return service.auditTrail(caller(u, r), id).stream().map(AuditEntry::from).toList();
    }
}
