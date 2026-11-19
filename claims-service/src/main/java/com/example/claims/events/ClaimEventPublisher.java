package com.example.claims.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes to Kafka only AFTER the database transaction commits, so we never announce
 * a change that was rolled back.
 *
 * Known trade-off: the DB commit and the Kafka send are not atomic. If the process dies
 * (or the broker is down) between the two, the event is lost. The production fix is the
 * transactional outbox pattern; see docs/DECISIONS.md.
 */
@Component
public class ClaimEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(ClaimEventPublisher.class);

    private final KafkaTemplate<String, Object> kafka;
    private final String topic;

    public ClaimEventPublisher(KafkaTemplate<String, Object> kafka,
                               @Value("${app.kafka.claims-topic}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ClaimEvent event) {
        try {
            kafka.send(topic, event.claimId().toString(), event).whenComplete((result, ex) -> {
                if (ex != null) log.error("Failed to publish {} for claim {}", event.eventType(), event.claimNumber(), ex);
            });
        } catch (Exception e) {
            // Never fail the user's request because the broker is unavailable.
            log.error("Could not publish {} for claim {}", event.eventType(), event.claimNumber(), e);
        }
    }
}
