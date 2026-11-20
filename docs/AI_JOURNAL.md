# AI working journal

A running log of what I asked the AI (Claude), what I accepted, what I challenged or had to fix,
and why. Written from the actual working session, 20 Nov 2026.

## Summary
- AI wrote nearly all of the code. I ran it, tested it, and fixed the environment problems.
- I mostly accepted the AI's design. The places where I had to verify, correct or work around
  its output are listed below, along with what is still unverified.

---

## 1. Understanding the brief
- **Asked:** whether the assessment could be done with AI, and how.
- **AI said:** yes, since the brief expects AI-driven code generation, but I must be able to
  defend every line. It proposed a plan before writing code.
- **Accepted:** backend first, frontend after, and a README plus a decisions document.

## 2. Architecture
- **AI proposed:** two services, `claims-service` (system of record, REST) and `exposure-service`
  (Kafka consumer, liability read model); REST for things a user waits on, Kafka for events that
  already happened; a claim state machine; manager exposure summary.
- **Accepted:** all of it. Reasoning I can give: the claim workflow needs strong consistency,
  while exposure reporting is read-heavy and can be eventually consistent, so they scale and
  fail separately. Splitting further would add network hops with no benefit at this size.
- **Assumptions the AI made that I should be able to explain** (listed in `docs/DECISIONS.md`):
  - the six markets (SG, MY, HK, TH, ID, VN), since the brief does not name them;
  - exposure = estimate while open, approved amount once approved, zero when rejected/settled;
  - identity via `X-User-Id` / `X-User-Role` headers instead of real login.

## 3. Choosing the stack
- **Asked:** which backend stack is easier and good, C#/.NET or Java/Spring Boot?
- **AI said:** Spring Boot, because H2, spring-kafka and springdoc need little setup, and
  `.NET` is also fine; pick whichever I can explain line by line.
- **Decision:** Spring Boot.

## 4. Code generation
- AI generated the domain model, services, REST API, Kafka publisher/consumer, tests,
  docker-compose, and the Angular frontend.
- **Design choices in the generated code I need to be able to explain:**
  - All legal status transitions live in one enum (`ClaimStatus`) and are enforced by the
    `Claim` class, so the rules are in one place and easy to unit test.
  - JPA `@Version` (optimistic locking) stops two officers picking up the same claim; the
    `revision` field on events lets the consumer ignore duplicates and old events.
  - Events are sent after the database commit, and failures are logged, not thrown, so a
    Kafka outage does not break the API.
  - Authorization is checked in the service layer, not only in controllers.

## 5. Problems and how they were handled
- **AI mistake, caught in the session:** a shell command did not expand `{domain,api,...}`, so
  the first Java files landed in the wrong folder and their packages did not match. The AI
  noticed when inspecting the file tree and fixed it in a separate commit.
- **Limits of AI verification:** the AI's sandbox had no Maven. It only compiled and ran the
  plain-Java domain rules and the exposure calculation (20 checks passed). It could not run the
  Spring, JPA or Kafka wiring, and said so.
- **My environment problems (solved while running the project):**
  - Docker Desktop install failed until WSL / Virtual Machine Platform was enabled;
  - I had Java 25 but the project targets Java 21, so I installed 21 alongside it;
  - Maven was not installed or on PATH;
  - the zip extracted into a folder inside a folder, so Maven could not find `pom.xml`.
- **Testing I did myself:**
  - `mvn test`: 13 tests passed (7 claims, 6 exposure);
  - started Kafka and both services, then used Swagger to submit, assign, review, approve and
    settle a claim, and checked the exposure service updated at each step;
  - submitting an amount of 0 returned a 400 with a clear message, which confirmed validation;
  - tested the Angular app in the browser as claimant, officer and manager, including the
    information-request loop (officer asks, claimant answers, claim returns to review).

## 6. What I did not do, and known weaknesses
- **Dual write:** the database commit and the Kafka send are not atomic. A crash between them
  loses an event. The fix is a transactional outbox, which I chose not to build in the time.
- **Auth is a header stub**, not a real identity provider.
- **Tests:** no integration tests with a real Kafka and no frontend tests.
- **`scripts/demo.sh`** was written but never run by me or the AI, so it is untested.
- **Workload** is computed in memory, which is fine at this scale but not at real volume.
- No notifications service, file uploads, or pagination.

## 7. Things I would change next
1. Add the outbox pattern so events cannot be lost.
2. Replace the header stub with real authentication (JWT/OIDC), keeping the service-layer checks.
3. Add Testcontainers integration tests for the Kafka flow, and component tests for the UI.
4. Move workload reporting into SQL aggregates or its own read model.
