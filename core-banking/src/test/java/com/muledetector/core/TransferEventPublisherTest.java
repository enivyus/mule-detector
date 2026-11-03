package com.muledetector.core;

import com.muledetector.core.account.Account;
import com.muledetector.core.account.AccountService;
import com.muledetector.core.transfer.TransferService;
import com.muledetector.core.web.ApiException;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.mock.MockProducerFactory;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real KafkaTemplate over Kafka's in-memory MockProducer: no broker, and no Mockito (unsupported on JDK 25). */
@SpringBootTest(properties = "spring.kafka.admin.auto-create=false")
@ActiveProfiles("kafka")
class TransferEventPublisherTest {

    static final MockProducer<String, String> producer =
            new MockProducer<>(true, new StringSerializer(), new StringSerializer());

    @TestConfiguration
    static class FakeBroker {
        @Bean
        KafkaTemplate<String, String> kafkaTemplate() {
            return new KafkaTemplate<>(new MockProducerFactory<>(() -> producer));
        }
    }

    @Autowired AccountService accountService;
    @Autowired TransferService transferService;

    @Test
    void publishesCommittedTransfersOnlyAsPlainJson() {
        Account a = accountService.open("Publisher");
        producer.clear();

        transferService.deposit(UUID.randomUUID().toString(), a.getAccountNo(), 1_000);
        assertThatThrownBy(() -> transferService.withdraw(UUID.randomUUID().toString(), a.getAccountNo(), 5_000))
                .isInstanceOf(ApiException.class);  // insufficient funds: rolled back, must not be published

        List<ProducerRecord<String, String>> sent = producer.history();
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).topic()).isEqualTo("transactions");
        assertThat(sent.get(0).headers().toArray()).isEmpty();  // no __TypeId__: other services must not need our classes
        assertThat(sent.get(0).value())
                .contains("\"toAccountNo\":\"" + a.getAccountNo() + "\"")
                .contains("\"kind\":\"DEPOSIT\"")
                .containsPattern("\"occurredAt\":\"\\d{4}-\\d{2}-\\d{2}T")  // ISO string, not epoch seconds
                .contains("\"toAccountType\":\"CUSTOMER\"");
    }
}
