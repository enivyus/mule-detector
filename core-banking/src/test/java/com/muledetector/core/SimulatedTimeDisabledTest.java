package com.muledetector.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Without the sim profile, clients must never be able to choose a booking time. */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "CHANNEL")
class SimulatedTimeDisabledTest {

    @Autowired MockMvc mvc;

    @Test
    void rejectsSimulatedTimeOutsideSimProfile() throws Exception {
        mvc.perform(post("/api/accounts").header("X-Simulated-Time", "2026-10-01T03:15:00+07:00")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ownerName\":\"Sim\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SIM_TIME_DISABLED"));
    }
}
