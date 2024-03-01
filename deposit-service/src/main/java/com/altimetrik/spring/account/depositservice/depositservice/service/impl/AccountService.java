package com.altimetrik.spring.account.depositservice.depositservice.service.impl;

import com.altimetrik.spring.account.depositservice.depositservice.model.Account;

import com.altimetrik.spring.account.depositservice.depositservice.model.Transaction;
import com.altimetrik.spring.account.depositservice.depositservice.queue.util.JmsQueueSender;
import com.altimetrik.spring.account.depositservice.depositservice.repository.IAccountRepository;
import com.altimetrik.spring.account.depositservice.depositservice.repository.ITransactionRepository;
import com.altimetrik.spring.account.depositservice.depositservice.service.IAccountService;
import com.github.loki4j.client.util.Loki4jLogger;

import org.slf4j.LoggerFactory;
import com.github.loki4j.slf4j.marker.LabelMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AccountService implements IAccountService{

    private final Logger LOG = LoggerFactory.getLogger(AccountService.class);
    @Autowired
    private IAccountRepository accountRepository;
    @Autowired
    private ITransactionRepository transactionRepository;

    @Autowired
    private JmsQueueSender jmsQueueSender;

    @Override
    public Account create(Account account)
    {
        if(!existsAccountByName(account.getName()))
        {
            LOG.info("New User "+account.getName()+" has been created successfully");
            return accountRepository.save(account);
            
        }
        else
        {
            LOG.info("User "+account.getName()+" already exists!");
            throw new IllegalArgumentException("Account already exists");
        }

    }

    @Override
    public Account update(Account account)
    {
        if (existsAccountByName(account.getName()))
        {
            Account account2 = accountRepository.save(account);
            Transaction transaction = new Transaction(account.getName(), account.getBalance(), "Credit", account.getName(), new Date());
            transactionRepository.save(transaction);
            LOG.info("User "+account.getName()+" has been updated successfully");
            return accountRepository.save(account2);

        }
        else
            LOG.info("User "+account.getName()+" does not exist");
            throw new IllegalArgumentException("Account Does Not Exists");
    }

    @Override
    public List<Account> getAll()
    {
        return accountRepository.getAll();
    }

    @Override
    public List<Transaction> getTransactions()
    {
        return transactionRepository.getTransactions();
    }

    @Override
    public Boolean existsAccountByName(String name)
    {
        return accountRepository.existsAccountByName(name);
    } 

    @Override
    public Account getByName(String name)
    {
        return accountRepository.getByName(name);
    }

    @Override
    public List<Transaction> getTransactionByName(String name, String recipient) {
        return transactionRepository.getTransactionByName(name,recipient);
    }
}


