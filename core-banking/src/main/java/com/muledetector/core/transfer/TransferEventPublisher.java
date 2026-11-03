package com.muledetector.core.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Forwards committed transfers to Redpanda as plain JSON (no type headers, ISO timestamps).
 * Only active with the "kafka" profile, so tests and a Postgres-only run never wait on a broker.
 */
@Configuration
@Profile("kafka")
public class TransferEventPublisher {

    public static final String TOPIC = "transactions";
    private static final Logger log = LoggerFactory.getLogger(TransferEventPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    public TransferEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper json) {
        this.kafka = kafka;
        this.json = json;
    }

    // shortcut: one partition keeps every account's in/out events in order (a transfer touches two accounts,
    // so no single key works); move to per-account keys with a reordering consumer if throughput matters
    @Bean
    NewTopic transactionsTopic() {
        return TopicBuilder.name(TOPIC).partitions(1).replicas(1).build();
    }

    // shortcut: an event is lost if the app dies between commit and send; upgrade to a transactional outbox
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(TransferCompletedEvent event) throws JsonProcessingException {
        kafka.send(TOPIC, event.fromAccountNo(), json.writeValueAsString(event))
                .whenComplete((r, e) -> {
                    if (e != null) log.error("Failed to publish transfer {}", event.transferId(), e);
                });
    }
}
