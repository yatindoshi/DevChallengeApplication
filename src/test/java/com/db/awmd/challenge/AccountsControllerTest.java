package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void prepareMockMvc() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        // Reset the existing accounts before each test.
        accountsService.getAccountsRepository().clearAccounts();
    }

    @Test
    public void createAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        Account account = accountsService.getAccount("Id-123");
        assertThat(account.getAccountId()).isEqualTo("Id-123");
        assertThat(account.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    public void createDuplicateAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNoAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNoBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNoBody() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNegativeBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountEmptyAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void getAccount() throws Exception {
        String uniqueAccountId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
        this.accountsService.createAccount(account);
        this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
                .andExpect(status().isOk())
                .andExpect(
                        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
    }

    @Test
    public void transferAmount() throws Exception {
        Account transferor = new Account("acc-1", new BigDecimal("100.0"));
        Account transferee = new Account("acc-2", new BigDecimal("100.0"));
        this.accountsService.createAccount(transferor);
        this.accountsService.createAccount(transferee);
        this.mockMvc.perform(post("/v1/accounts/transfer?accountFrom=acc-1&accountTo=acc-2&amount=5"))
                .andExpect(status().isAccepted());
        assertEquals(transferor.getBalance().doubleValue(), 95.0, 0);
        assertEquals(transferee.getBalance().doubleValue(), 105.0, 0);
    }

    @Test
    public void invalidAccountFrom() throws Exception {
        Account transferor = new Account("acc-1", new BigDecimal("100.0"));
        Account transferee = new Account("acc-2", new BigDecimal("100.0"));
        this.accountsService.createAccount(transferor);
        this.accountsService.createAccount(transferee);
        this.mockMvc.perform(post("/v1/accounts/transfer?accountFrom=acc-3&accountTo=acc-2&amount=5"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().string("accountFrom doesn't represent valid account"));
        ;
    }

    @Test
    public void invalidAccountTo() throws Exception {
                Account transferor = new Account("acc-1", new BigDecimal("100.0"));
        Account transferee = new Account("acc-2", new BigDecimal("100.0"));
        this.accountsService.createAccount(transferor);
        this.accountsService.createAccount(transferee);
        this.mockMvc.perform(post("/v1/accounts/transfer?accountFrom=acc-1&accountTo=acc-3&amount=5"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().string("accountTo doesn't represent valid account"));
        ;
    }

    @Test
    public void negativeAmountTransfer() throws Exception {
        Account transferor = new Account("acc-1", new BigDecimal("100.0"));
        Account transferee = new Account("acc-2", new BigDecimal("100.0"));
        this.accountsService.createAccount(transferor);
        this.accountsService.createAccount(transferee);
        this.mockMvc.perform(post("/v1/accounts/transfer?accountFrom=acc-1&accountTo=acc-2&amount=-5"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().string("Amount to transfer should be more than 0"));
    }

    @Test
    public void sameAccountTransfer() throws Exception {
        Account transferor = new Account("acc-1", new BigDecimal("100.0"));
        Account transferee = new Account("acc-2", new BigDecimal("100.0"));
        this.accountsService.createAccount(transferor);
        this.accountsService.createAccount(transferee);
        this.mockMvc.perform(post("/v1/accounts/transfer?accountFrom=acc-1&accountTo=acc-1&amount=-5"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().string("transfer is not possible with same account"));
        ;
    }

    @Test
    public void limitExhausting() throws Exception {
        Account transferor = new Account("acc-1", new BigDecimal("100.0"));
        Account transferee = new Account("acc-2", new BigDecimal("100.0"));
        this.accountsService.createAccount(transferor);
        this.accountsService.createAccount(transferee);
        this.mockMvc.perform(post("/v1/accounts/transfer?accountFrom=acc-1&accountTo=acc-2&amount=105"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().string("Amount transferred shouldn't be more than balance"));
        ;
    }
}
