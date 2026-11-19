# Claims Platform (take-home)

Claimants report incidents and track claims; claims staff pick up, assess and settle them;
managers see workload and outstanding liability. Backend-first: two Spring Boot services
communicating over REST (client-facing) and Kafka (service-to-service events).

```
 Angular (planned)            REST                       Kafka: claims.events
 ──────────────▶  claims-service :8080  ───────────────────────────────▶  exposure-service :8081
                  claim lifecycle, queue,                                 read model of outstanding
                  workload, audit trail                                   liability, manager summary
                  (H2)                                                    (H2)
```

## Run it locally

Requirements: JDK 21, Maven 3.9+, Docker (for Kafka only).

```bash
docker compose up -d                              # Kafka on :9092
mvn -q test                                       # unit tests for both services
mvn -pl claims-service spring-boot:run            # terminal 1  -> :8080
mvn -pl exposure-service spring-boot:run          # terminal 2  -> :8081
./scripts/demo.sh                                 # terminal 3  -> walks a claim end to end (needs curl + jq)
```

Swagger UI: http://localhost:8080/swagger-ui.html and http://localhost:8081/swagger-ui.html

The claims API still works if Kafka is down (events are logged as failed); exposure just
won't update until events flow again.

## Identity (stand-in for real auth)

Send `X-User-Id` and `X-User-Role` (`CLAIMANT`, `OFFICER` or `MANAGER`) on every request.
Authorization is enforced in the service layer: claimants see only their own claims, officers
act only on claims assigned to them, managers get read-only team views.

## Main endpoints (claims-service)

| Who | Endpoint | Purpose |
|---|---|---|
| Claimant | `POST /api/claims` | Report an incident |
| Claimant | `GET /api/claims/mine`, `GET /api/claims/{id}` | Track claims |
| Claimant | `POST /api/claims/{id}/info-requests/{rid}/answer` | Provide requested information |
| Officer | `GET /api/claims/queue` | Unassigned claims, oldest first |
| Officer | `POST /api/claims/{id}/assign` → `start-review` → `info-requests` / `decision` → `settle` | Work a claim |
| Officer/Manager | `GET /api/staff/workload` | Open/decided counts, avg hours to decision per officer |
| Manager | `GET :8081/api/exposure/summary` | Outstanding liability, by market and status |

Design decisions and trade-offs: [docs/DECISIONS.md](docs/DECISIONS.md).
AI working log: [docs/AI_JOURNAL.md](docs/AI_JOURNAL.md).
