package com.muledetector.rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class TransactionListener extends AbstractConsumerSeekAware {

    static final String TOPIC = "transactions";

    private final RuleEngine engine;
    private final ObjectMapper json;
    private final AutoFreeze autoFreeze;  // null unless the autofreeze profile is on
    private volatile Instant rebuildStartedAt = Instant.MIN;

    public TransactionListener(RuleEngine engine, ObjectMapper json, ObjectProvider<AutoFreeze> autoFreeze) {
        this.engine = engine;
        this.json = json;
        this.autoFreeze = autoFreeze.getIfAvailable();
    }

    /** Same definition as core-banking's, so whichever service starts first creates it. */
    @Bean
    static NewTopic transactionsTopic() {
        return TopicBuilder.name(TOPIC).partitions(1).replicas(1).build();
    }

    @KafkaListener(topics = TOPIC)
    public void on(String message, @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long publishedAtMillis)
            throws JsonProcessingException {
        TransferEvent e = json.readValue(message, TransferEvent.class);
        var reached = engine.onEvent(e);
        // Only act on transfers published after this rebuild started: replaying history must not freeze again
        // (an investigator may have unfrozen a false positive since then)
        if (autoFreeze != null && !Instant.ofEpochMilli(publishedAtMillis).isBefore(rebuildStartedAt)) {
            reached.forEach(no -> autoFreeze.freeze(no, e.occurredAt(), engine.suspects().get(no)));
        }
    }

    // shortcut: rule state is in memory, so every (re)assignment replays the whole topic from scratch;
    // keep state in a store (RocksDB / Kafka Streams state store) once the topic is too long to replay on start.
    // Transfers published between the last record seen and a rebalance count as history and are never frozen.
    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        super.onPartitionsAssigned(assignments, callback);
        rebuildStarted(Instant.now());
        callback.seekToBeginning(assignments.keySet());
    }

    void rebuildStarted(Instant at) {
        engine.reset();
        rebuildStartedAt = at;
    }
}
