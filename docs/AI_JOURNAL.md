# AI working journal

Running log of what I asked the AI, what I accepted, challenged or overrode, and why.
(Draft entries from the working session. Edit so they reflect your own reasoning.)

## 1. Choosing the stack
- **Asked:** which backend stack is easier, C#/.NET or Spring Boot?
- **Result:** AI recommended Spring Boot (H2, spring-kafka, springdoc need little setup) but said
  to pick whichever I can explain line by line.
- **Decision:** Spring Boot.
- **My notes:** _why this suits me:_

## 2. Architecture proposal
- **Asked:** to decompose the brief.
- **Proposed:** 2 services (claims, exposure), REST for user actions, Kafka for events, a
  state machine for the lifecycle, a manager exposure summary.
- **Accepted:** the 2-service split and REST/Kafka division.
- **My notes:** _anything I changed or would challenge:_

## 3. Domain and API generation
- **Accepted:** state machine in a single enum, aggregate methods that enforce transitions,
  audit table, DTOs instead of exposing entities.
- **Challenged / to verify myself:** whether Claim `revision` or JPA `@Version` should be used
  on events. Kept both: `@Version` is for locking, `revision` is the business sequence on events.

## 4. Kafka publishing
- **AI approach:** publish in an `AFTER_COMMIT` listener.
- **Known weakness (documented):** dual-write problem; outbox pattern is the proper fix.
- **Decision:** accept for the sprint and document honestly in DECISIONS.md.
- **My notes:** _would I add the outbox if I had more time? how?_

## 5. Bug caught during the session
- A shell `mkdir -p {a,b,c}` did not brace-expand, so the first domain files landed in the
  wrong directory and packages did not match. Caught by inspecting the tree, fixed in a
  follow-up commit. Lesson: verify generated output instead of trusting it.

## 6. Verification limits
- The sandbox had no Maven, so only the pure-Java domain rules and exposure calculator were
  compiled and run (with stub annotations). The Spring/JPA/Kafka wiring was **not** executed
  by the AI and must be run locally (`mvn test`, then run both services).
- **My notes:** _errors found when I first ran it and how I fixed them:_
