package com.altimetrik.spring.account.depositservice.depositservice.service.impl;


import com.altimetrik.spring.account.depositservice.depositservice.model.Account;
import com.altimetrik.spring.account.depositservice.depositservice.model.Transaction;
import com.altimetrik.spring.account.depositservice.depositservice.queue.util.JmsQueueSender;
import com.altimetrik.spring.account.depositservice.depositservice.repository.IAccountRepository;
import com.altimetrik.spring.account.depositservice.depositservice.repository.ITransactionRepository;
import com.altimetrik.spring.account.depositservice.depositservice.service.IDepositService;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.altimetrik.spring.account.depositservice.depositservice.var.QueueName;
import com.github.loki4j.client.util.Loki4jLogger;

import org.slf4j.LoggerFactory;
import com.github.loki4j.slf4j.marker.LabelMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;
import java.util.HashMap;

@Service
public class DepositService implements IDepositService{
    private final Logger LOG = LoggerFactory.getLogger(AccountService.class);


    @Autowired
    private IAccountRepository accountRepository;

    @Autowired
    private ITransactionRepository transactionRepository;


    @Autowired
    private JmsQueueSender jmsQueueSender;

    public void depositBalance(String name, Double balance)
    {
        String transactionId = String.valueOf(new Date().getTime());
        Account account = accountRepository.getByName(name);
        Transaction transaction2 = new Transaction(name, balance, "credit", name, new Date());
        if (account !=null && balance > 0D)
        {
            if (account.getBalance() == null){
                account.setBalance(balance);
                accountRepository.save(account);
                transactionRepository.save(transaction2);
                LOG.info("Account "+account.getName()+" has been deposited with " + balance+" GBP Succesfully, transaction ID is "+transactionId);
                jmsQueueSender.sendMessage(QueueName.DEPOSIT_ACT,getSuccessDepositAcknowledgement(transactionId));
            }
            else
            {
                account.setBalance(account.getBalance()+balance);
                accountRepository.save(account);
                transactionRepository.save(transaction2);
                LOG.info("Account "+account.getName()+" has been deposited with " + balance+" GBP Succesfully, transaction ID is "+transactionId);
                jmsQueueSender.sendMessage(QueueName.DEPOSIT_ACT,getSuccessDepositAcknowledgement(transactionId));
            }

        }
        else{
            LOG.error("Account not found or Invalid Balance");
            jmsQueueSender.sendMessage(QueueName.DEPOSIT_ACT,getFailedDepositAcknowledgement(transactionId));
        }

    }

    private HashMap<String, Object> getSuccessDepositAcknowledgement(String transactionId)
    {
        HashMap<String, Object> depositAcknowledgementMap = new HashMap<>();
        depositAcknowledgementMap.put("transactionId",transactionId);
        depositAcknowledgementMap.put("status","success");
        depositAcknowledgementMap.put("message","Transaction Completed");
        return depositAcknowledgementMap;
    }

    private HashMap<String, Object> getFailedDepositAcknowledgement(String transactionId)
    {
        HashMap<String, Object> depositAcknowledgementMap = new HashMap<>();
        depositAcknowledgementMap.put("transactionId",transactionId);
        depositAcknowledgementMap.put("status","failed");
        depositAcknowledgementMap.put("message","Account not found or Invalid Balance");
        return depositAcknowledgementMap;
    }





    
}

