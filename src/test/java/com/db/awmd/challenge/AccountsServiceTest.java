package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InvalidTransferRequestException;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.service.AccountsService;

import java.math.BigDecimal;

import com.db.awmd.challenge.service.EmailNotificationService;
import com.db.awmd.challenge.service.NotificationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

    @Autowired
    private AccountsService accountsService;

    @Test
    public void addAccount() throws Exception {
        Account account = new Account("Id-123");
        account.setBalance(new BigDecimal(1000));
        this.accountsService.createAccount(account);

        assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
    }

    @Test
    public void addAccount_failsOnDuplicateId() throws Exception {
        String uniqueId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueId);
        this.accountsService.createAccount(account);

        try {
            this.accountsService.createAccount(account);
            fail("Should have failed when adding duplicate account");
        } catch (DuplicateAccountIdException ex) {
            assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
        }
    }

    @Test
    public void transfer() {
        Account transferor = new Account("id-1", new BigDecimal(100));
        Account transferee = new Account("id-2", new BigDecimal(100));
        AccountsRepository accountsRepository = Mockito.mock(AccountsRepository.class);
        Mockito.when(accountsRepository.getAccount("id-1")).thenReturn(transferor);
        Mockito.when(accountsRepository.getAccount("id-2")).thenReturn(transferee);
        EmailNotificationService notificationService = Mockito.mock(EmailNotificationService.class);
        AccountsService accountsService = new AccountsService(accountsRepository, notificationService);
        accountsService.transfer("id-1", "id-2", new BigDecimal(5));

        Mockito.verify(notificationService).notifyAboutTransfer(transferee, "Your account has been credited with amount 5 from id-1");
        Mockito.verify(notificationService).notifyAboutTransfer(transferor, "Your account has been debited with amount 5 and has been transferred to id-2");
        assertEquals(transferee.getBalance().doubleValue(), 105.0, 0);
        assertEquals(transferor.getBalance().doubleValue(), 95.0, 0);
    }

    @Test
    public void invalidAccountFrom() {
        try {
            Account transferor = new Account("id-1", new BigDecimal(100));
            Account transferee = new Account("id-2", new BigDecimal(100));
            AccountsRepository accountsRepository = Mockito.mock(AccountsRepository.class);
            Mockito.when(accountsRepository.getAccount("id-1")).thenReturn(transferor);
            EmailNotificationService notificationService = Mockito.mock(EmailNotificationService.class);
            AccountsService accountsService = new AccountsService(accountsRepository, notificationService);
            accountsService.transfer("id-4", "id-2", new BigDecimal(5));
        } catch (InvalidTransferRequestException e) {
            assertEquals(e.getMessage(), "accountFrom doesn't represent valid account");
        }
    }

    @Test
    public void invalidAccountTo() {
        try {
            Account transferor = new Account("id-1", new BigDecimal(100));
            Account transferee = new Account("id-2", new BigDecimal(100));
            AccountsRepository accountsRepository = Mockito.mock(AccountsRepository.class);
            Mockito.when(accountsRepository.getAccount("id-1")).thenReturn(transferor);
            Mockito.when(accountsRepository.getAccount("id-2")).thenReturn(transferee);
            EmailNotificationService notificationService = Mockito.mock(EmailNotificationService.class);
            AccountsService accountsService = new AccountsService(accountsRepository, notificationService);
            accountsService.transfer("id-1", "id-4", new BigDecimal(5));
        } catch (InvalidTransferRequestException e) {
            assertEquals(e.getMessage(), "accountTo doesn't represent valid account");
        }
    }

    @Test
    public void invalidAmountToTransfer() {
        try {
            Account transferor = new Account("id-1", new BigDecimal(100));
            Account transferee = new Account("id-2", new BigDecimal(100));
            AccountsRepository accountsRepository = Mockito.mock(AccountsRepository.class);
            Mockito.when(accountsRepository.getAccount("id-1")).thenReturn(transferor);
            Mockito.when(accountsRepository.getAccount("id-2")).thenReturn(transferee);
            EmailNotificationService notificationService = Mockito.mock(EmailNotificationService.class);
            AccountsService accountsService = new AccountsService(accountsRepository, notificationService);
            accountsService.transfer("id-1", "id-2", new BigDecimal(-5));
        } catch (InvalidTransferRequestException e) {
            assertEquals(e.getMessage(), "Amount to transfer should be more than 0");
        }
    }

    @Test
    public void sameAccountTransfer() {
        try {
            AccountsRepository accountsRepository = Mockito.mock(AccountsRepository.class);
            EmailNotificationService notificationService = Mockito.mock(EmailNotificationService.class);
            AccountsService accountsService = new AccountsService(accountsRepository, notificationService);
            accountsService.transfer("id-1", "id-1", new BigDecimal(-5));
        } catch (InvalidTransferRequestException e) {
            assertEquals(e.getMessage(), "transfer is not possible with same account");
        }
    }

    @Test
    public void transferMoreThanLimit() {
        try {
            Account transferor = new Account("id-1", new BigDecimal(100));
            Account transferee = new Account("id-2", new BigDecimal(100));
            AccountsRepository accountsRepository = Mockito.mock(AccountsRepository.class);
            Mockito.when(accountsRepository.getAccount("id-1")).thenReturn(transferor);
            Mockito.when(accountsRepository.getAccount("id-2")).thenReturn(transferee);
            EmailNotificationService notificationService = Mockito.mock(EmailNotificationService.class);
            AccountsService accountsService = new AccountsService(accountsRepository, notificationService);
            accountsService.transfer("id-1", "id-2", new BigDecimal(105));
        } catch (InvalidTransferRequestException e) {
            assertEquals(e.getMessage(), "Amount transferred shouldn't be more than balance");
        }
    }

}
