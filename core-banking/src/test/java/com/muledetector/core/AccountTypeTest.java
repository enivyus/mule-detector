package com.muledetector.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountTypeTest {

    @Autowired MockMvc mvc;

    private ResultActions open(String body) throws Exception {
        return mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void defaultsToCustomerAndAcceptsBusiness() throws Exception {
        open("{\"ownerName\":\"Person\"}").andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("CUSTOMER"));
        open("{\"ownerName\":\"Shop\",\"type\":\"BUSINESS\"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("BUSINESS"));
    }

    @Test
    void refusesToOpenSystemAccounts() throws Exception {
        open("{\"ownerName\":\"Vault 2\",\"type\":\"SYSTEM\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ACCOUNT_TYPE"));
    }
}
