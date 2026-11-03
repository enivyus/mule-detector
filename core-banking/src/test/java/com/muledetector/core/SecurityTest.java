package com.muledetector.core;

import com.muledetector.core.account.Account;
import com.muledetector.core.account.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real users from the test config, sent as HTTP Basic, against the real filter chain. */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired MockMvc mvc;
    @Autowired AccountService accountService;

    static MockHttpServletRequestBuilder as(String user, MockHttpServletRequestBuilder request) {
        return request.with(httpBasic(user, user + "-test"));
    }

    static MockHttpServletRequestBuilder reason(MockHttpServletRequestBuilder request, String reason) {
        return request.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + reason + "\"}");
    }

    String newAccount() {
        return accountService.open("Security", Account.Type.CUSTOMER, null).getAccountNo();
    }

    @Test
    void rejectsMissingOrWrongCredentialsWithoutABrowserPrompt() throws Exception {
        String no = newAccount();
        mvc.perform(get("/api/accounts/" + no)).andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
        mvc.perform(get("/api/accounts/" + no).with(httpBasic("channel", "wrong"))).andExpect(status().isUnauthorized());
    }

    @Test
    void eachRoleCanDoOnlyItsOwnJob() throws Exception {
        String no = newAccount();
        String transfer = "{\"fromAccountNo\":\"" + no + "\",\"toAccountNo\":\"1\",\"amountSatang\":1}";

        mvc.perform(as("channel", reason(post("/api/admin/accounts/" + no + "/freeze"), "x"))).andExpect(status().isForbidden());
        mvc.perform(as("investigator", post("/api/transfers").header("Idempotency-Key", "sec-1")
                .contentType(MediaType.APPLICATION_JSON).content(transfer))).andExpect(status().isForbidden());
        mvc.perform(as("rule-engine", get("/api/accounts/" + no))).andExpect(status().isForbidden());

        mvc.perform(as("rule-engine", reason(post("/api/admin/accounts/" + no + "/freeze"), "auto")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FROZEN"));
        // Lifting a freeze always takes a person
        mvc.perform(as("rule-engine", reason(post("/api/admin/accounts/" + no + "/unfreeze"), "auto")))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditRecordsTheLoggedInUserAndReasonForEachRealChange() throws Exception {
        String no = newAccount();
        String freeze = "/api/admin/accounts/" + no + "/freeze";

        mvc.perform(as("investigator", reason(post(freeze), "victim called the hotline"))).andExpect(status().isOk());
        mvc.perform(as("investigator", reason(post(freeze), "again"))).andExpect(status().isOk());  // no change, no row
        mvc.perform(as("investigator", reason(post("/api/admin/accounts/" + no + "/unfreeze"), "owner proved the income")))
                .andExpect(status().isOk());

        mvc.perform(as("investigator", get("/api/admin/accounts/" + no + "/audit")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("UNFREEZE"))
                .andExpect(jsonPath("$[0].reason").value("owner proved the income"))
                .andExpect(jsonPath("$[1].action").value("FREEZE"))
                .andExpect(jsonPath("$[1].actor").value("investigator"));
        mvc.perform(as("channel", get("/api/admin/accounts/" + no + "/audit"))).andExpect(status().isForbidden());
    }

    @Test
    void freezeNeedsAReason() throws Exception {
        String no = newAccount();
        mvc.perform(as("investigator", reason(post("/api/admin/accounts/" + no + "/freeze"), " ")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(as("investigator", post("/api/admin/accounts/" + no + "/freeze"))).andExpect(status().isBadRequest());
    }

    @Test
    void meNamesTheCallerAndRole() throws Exception {
        mvc.perform(as("investigator", get("/api/me")))
                .andExpect(jsonPath("$.username").value("investigator"))
                .andExpect(jsonPath("$.roles[0]").value("INVESTIGATOR"));
    }
}
