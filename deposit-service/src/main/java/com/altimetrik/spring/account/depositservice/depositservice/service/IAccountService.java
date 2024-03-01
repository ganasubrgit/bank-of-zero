package com.altimetrik.spring.account.depositservice.depositservice.service;

import com.altimetrik.spring.account.depositservice.depositservice.model.Account;
import com.altimetrik.spring.account.depositservice.depositservice.model.Transaction;

import java.util.List;

public interface IAccountService {
    
    Account create(Account account);
    Account update(Account account);
    List<Account> getAll();
    List<Transaction> getTransactions();
    Boolean existsAccountByName(String name);
    Account getByName(String name);
    List<Transaction> getTransactionByName(String name, String recipient);
}
