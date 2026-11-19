# Design decisions

## Scope
Backend first, as the brief suggests. Frontend is the last item and only built if time allows.

## Two services, not one
- **claims-service** owns the claim lifecycle and is the system of record. It needs strong
  consistency (state machine, optimistic locking, audit).
- **exposure-service** answers "what is our outstanding liability?" It is a read-heavy,
  eventually-consistent projection with a different scaling and failure profile: a slow
  dashboard query must never slow down someone submitting a claim.
- I did **not** split further (e.g. separate assessment/notification services). With one
  team and a 2–3 hour budget that would add network hops without adding value.

## REST vs Kafka
- **REST** for everything a user waits on and needs an immediate answer to (submit, assign,
  decide). The caller needs a success/failure result now.
- **Kafka** for facts that have already happened, consumed by other services that the user
  is not waiting for. Topic `claims.events`, keyed by claimId so one claim's events stay in
  order on one partition. Exposure is the first consumer; notifications would be the next.
- The event carries a snapshot plus `revision`. Consumers de-duplicate on revision, so
  at-least-once delivery is safe.

## Liability exposure definition
Rejected/settled = 0. Approved = approved amount. Anything else = claimant's estimate
(a reserve). This is my assumption about what "outstanding exposure" means and would be
confirmed with the business.

## Claim lifecycle
`SUBMITTED → ASSIGNED → UNDER_REVIEW ⇄ INFO_REQUESTED`, then `APPROVED → SETTLED` or
`REJECTED`. Rules live in one enum (`ClaimStatus`) and are enforced by the `Claim` aggregate.

## Concurrency
`@Version` optimistic locking on Claim: if two officers try to pick up the same claim, one
gets HTTP 409 instead of both "winning".

## Known trade-offs / what I'd do next
1. **Dual write.** The DB commit and Kafka send are not atomic (the event is sent in an
   `AFTER_COMMIT` hook). A crash in between loses the event. Fix: transactional outbox
   table + relay. Deliberately skipped for time; this is the first thing I'd add.
2. **Auth is a header stub**, not JWT/OIDC. Authorization logic is real and in the service
   layer, so swapping in a real identity provider only changes how `Caller` is built.
3. **Contract duplication.** `ClaimEvent` is copied into each service instead of a shared
   library, to keep services independently deployable. At scale I'd use a schema registry.
4. **Workload is computed in memory** from assigned claims; at volume it should be a SQL
   aggregate or its own projection.
5. **No notifications service, document uploads, or pagination** yet.
6. **Markets** (SG, MY, HK, TH, ID, VN) are an assumption; the brief only says "six".
7. **H2 file databases** for zero-setup local running; Postgres in a real deployment.
8. Integration tests (Testcontainers Kafka) not written; unit tests cover the domain rules,
   exposure calculation and consumer idempotency.
