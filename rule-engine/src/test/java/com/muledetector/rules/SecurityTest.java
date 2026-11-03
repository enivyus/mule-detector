package com.muledetector.rules;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
@Import({SecurityConfig.class, RuleEngine.class})
@TestPropertySource(properties = {"mulebank.passwords.investigator=inv", "mulebank.passwords.graph-service=graph"})
class SecurityTest {

    @Autowired MockMvc mvc;

    static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
    }

    @Test
    void alertsNeedALogin() throws Exception {
        mvc.perform(get("/alerts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/alerts").header("Authorization", basic("investigator", "wrong"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/alerts").header("Authorization", basic("investigator", "inv"))).andExpect(status().isOk());
        mvc.perform(get("/suspects").header("Authorization", basic("graph-service", "graph"))).andExpect(status().isOk());
    }
}
