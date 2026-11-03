package com.muledetector.rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TransactionListener extends AbstractConsumerSeekAware {

    static final String TOPIC = "transactions";

    private final RuleEngine engine;
    private final ObjectMapper json;

    public TransactionListener(RuleEngine engine, ObjectMapper json) {
        this.engine = engine;
        this.json = json;
    }

    /** Same definition as core-banking's, so whichever service starts first creates it. */
    @Bean
    static NewTopic transactionsTopic() {
        return TopicBuilder.name(TOPIC).partitions(1).replicas(1).build();
    }

    @KafkaListener(topics = TOPIC)
    public void on(String message) throws JsonProcessingException {
        engine.onEvent(json.readValue(message, TransferEvent.class));
    }

    // shortcut: rule state is in memory, so every (re)assignment replays the whole topic from scratch;
    // keep state in a store (RocksDB / Kafka Streams state store) once the topic is too long to replay on start
    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        super.onPartitionsAssigned(assignments, callback);
        engine.reset();
        callback.seekToBeginning(assignments.keySet());
    }
}
