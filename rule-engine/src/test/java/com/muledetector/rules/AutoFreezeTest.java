package com.muledetector.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** MockRestServiceServer stands in for core-banking (no Mockito: it cannot instrument classes on JDK 25). */
class AutoFreezeTest {

    static final Instant REBUILD = Instant.parse("2026-11-03T12:00:00Z");
    static final OffsetDateTime NIGHT = OffsetDateTime.parse("2026-10-10T01:00:00+07:00");

    final ObjectMapper json = JsonMapper.builder().findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
    final RestClient.Builder http = RestClient.builder();
    final MockRestServiceServer bank = MockRestServiceServer.bindTo(http).build();
    final TransactionListener listener = new TransactionListener(new RuleEngine(), json,
            new StaticListableBeanFactory(Map.of("autoFreeze", new AutoFreeze(http, "http://127.0.0.1:8080", "pw")))
                    .getBeanProvider(AutoFreeze.class));

    /** A young account receives 30,000 THB at night and passes 29,000 on: burst, pass-through, odd hours. */
    void mule(String account, Instant publishedAt) throws Exception {
        OffsetDateTime opened = NIGHT.minusDays(3);
        send(new TransferEvent(1, "V1", account, 3_000_000, NIGHT, "CUSTOMER", "CUSTOMER", opened), publishedAt);
        send(new TransferEvent(2, account, "X", 2_900_000, NIGHT.plusMinutes(10), "CUSTOMER", "CUSTOMER", NIGHT.minusYears(3)),
                publishedAt);
    }

    void send(TransferEvent e, Instant publishedAt) throws Exception {
        listener.on(json.writeValueAsString(e), publishedAt.toEpochMilli());
    }

    @Test
    void freezesOnceOnLiveTransfersButNeverWhileReplayingHistory() throws Exception {
        bank.expect(once(), requestTo("http://127.0.0.1:8080/api/admin/accounts/LIVE/freeze"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic " + Base64.getEncoder().encodeToString("rule-engine:pw".getBytes())))
                .andExpect(jsonPath("$.reason").value("Auto-freeze: new_account_burst, odd_hours, pass_through"))
                .andRespond(withSuccess());
        listener.rebuildStarted(REBUILD);

        mule("HISTORY", REBUILD.minusSeconds(60));  // published before the rebuild: must not freeze again
        mule("LIVE", REBUILD.plusSeconds(60));
        send(new TransferEvent(3, "LIVE", "Y", 1_000, NIGHT.plusMinutes(20), "CUSTOMER", "CUSTOMER", NIGHT),
                REBUILD.plusSeconds(61));           // a fourth night transfer: already frozen, no second call

        bank.verify();  // exactly one call, for LIVE; any other request fails the test
    }
}
