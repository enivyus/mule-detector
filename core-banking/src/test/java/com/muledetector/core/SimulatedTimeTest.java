package com.muledetector.core;

import com.muledetector.core.account.Account;
import com.muledetector.core.account.AccountRepository;
import com.muledetector.core.transfer.TransferRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sim")
class SimulatedTimeTest {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired TransferRepository transfers;

    @Test
    void simProfileBooksAccountsAndTransfersAtTheGivenTime() throws Exception {
        OffsetDateTime opened = OffsetDateTime.parse("2026-09-01T09:00:00+07:00");
        OffsetDateTime booked = OffsetDateTime.parse("2026-10-01T03:15:00+07:00");

        String body = mvc.perform(post("/api/accounts").header("X-Simulated-Time", opened.toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ownerName\":\"Sim\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String accountNo = body.replaceAll(".*\"accountNo\":\"(\\d+)\".*", "$1");

        mvc.perform(post("/api/accounts/" + accountNo + "/deposit").header("Idempotency-Key", "sim-dep-1")
                        .header("X-Simulated-Time", booked.toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amountSatang\":500}"))
                .andExpect(status().isOk());

        Account a = accounts.findByAccountNo(accountNo).orElseThrow();
        assertThat(a.getCreatedAt().toInstant()).isEqualTo(opened.toInstant());
        assertThat(transfers.findByIdempotencyKey("sim-dep-1").orElseThrow().getCreatedAt().toInstant())
                .isEqualTo(booked.toInstant());
    }

    @Test
    void rejectsMalformedSimulatedTime() throws Exception {
        mvc.perform(post("/api/accounts").header("X-Simulated-Time", "yesterday")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ownerName\":\"Sim\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SIM_TIME"));
    }
}
