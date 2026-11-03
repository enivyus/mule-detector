package com.muledetector.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.lang.reflect.RecordComponent;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** The other half of core-banking's TransferEventPublisherTest: a real event, read with Boot's ObjectMapper. */
@JsonTest
class TransferEventJsonTest {

    // Copied from topic "transactions" (rpk topic consume transactions -n 1)
    static final String EVENT = """
            {"transferId":1,"fromAccountNo":"0000000000","toAccountNo":"1741839944","amountSatang":24071,\
            "kind":"DEPOSIT","occurredAt":"2026-09-12T12:00:00+07:00","fromAccountType":"SYSTEM",\
            "toAccountType":"CUSTOMER","fromOpenedAt":"2026-11-03T13:37:59.181226Z","toOpenedAt":"2026-09-10T17:00:00Z"}""";

    @Autowired ObjectMapper json;

    @Test
    void readsEveryFieldOfARealEvent() throws Exception {
        TransferEvent e = json.readValue(EVENT, TransferEvent.class);

        for (RecordComponent c : TransferEvent.class.getRecordComponents()) {
            assertThat(c.getAccessor().invoke(e)).as(c.getName()).isNotNull();
        }
        assertThat(e.amountSatang()).isEqualTo(24_071);
        assertThat(e.fromSystem()).isTrue();
        assertThat(e.occurredAt().toInstant()).isEqualTo(OffsetDateTime.parse("2026-09-12T05:00:00Z").toInstant());
    }
}
