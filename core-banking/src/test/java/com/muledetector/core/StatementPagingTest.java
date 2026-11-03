package com.muledetector.core;

import com.muledetector.core.account.Account;
import com.muledetector.core.account.AccountService;
import com.muledetector.core.transfer.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "CHANNEL")
class StatementPagingTest {

    @Autowired MockMvc mvc;
    @Autowired AccountService accountService;
    @Autowired TransferService transferService;

    @Test
    void statementIsPagedNewestFirst() throws Exception {
        Account a = accountService.open("Paging", Account.Type.CUSTOMER, null);
        for (long amount = 100; amount <= 300; amount += 100) {
            transferService.deposit(UUID.randomUUID().toString(), a.getAccountNo(), amount, null);
        }
        String url = "/api/accounts/" + a.getAccountNo() + "/statement";

        mvc.perform(get(url).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].amountSatang").value(300))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        mvc.perform(get(url).param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].amountSatang").value(100));
    }

    @Test
    void rejectsOutOfRangePageSize() throws Exception {
        Account a = accountService.open("Paging", Account.Type.CUSTOMER, null);
        mvc.perform(get("/api/accounts/" + a.getAccountNo() + "/statement").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGE"));
    }
}
