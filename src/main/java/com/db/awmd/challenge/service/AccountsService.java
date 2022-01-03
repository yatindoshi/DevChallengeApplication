package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.InvalidTransferRequestException;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountsService {

    @Getter
    private final AccountsRepository accountsRepository;

    private final EmailNotificationService notificationService;

    @Autowired
    public AccountsService(AccountsRepository accountsRepository, EmailNotificationService notificationService) {
        this.accountsRepository = accountsRepository;
        this.notificationService = notificationService;
    }

    public void createAccount(Account account) {
        this.accountsRepository.createAccount(account);
    }

    public Account getAccount(String accountId) {
        return this.accountsRepository.getAccount(accountId);
    }

    public void transfer(String accountFrom, String accountTo, BigDecimal amount) {
        if (accountFrom.equals(accountTo)) {
            throw new InvalidTransferRequestException("transfer is not possible with same account");
        }
        Account transferorAccount = getAccount(accountFrom);
        if (transferorAccount == null) {
            throw new InvalidTransferRequestException("accountFrom doesn't represent valid account");
        }
        Account transfereeAccount = getAccount(accountTo);
        if (transfereeAccount == null) {
            throw new InvalidTransferRequestException("accountTo doesn't represent valid account");
        }
        if (amount.compareTo(new BigDecimal(0)) <= 0) {
            throw new InvalidTransferRequestException("Amount to transfer should be more than 0");
        }
        synchronized (this) {
            transferorAccount.debit(amount);
            transfereeAccount.credit(amount);
            notificationService.notifyAboutTransfer(transferorAccount, "Your account has been debited with amount " + amount + " and has been transferred to " + transfereeAccount.getAccountId());
            notificationService.notifyAboutTransfer(transfereeAccount, "Your account has been credited with amount " + amount + " from " + transferorAccount.getAccountId());
        }
    }
}
