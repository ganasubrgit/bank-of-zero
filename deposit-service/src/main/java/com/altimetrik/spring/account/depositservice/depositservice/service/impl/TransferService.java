package com.altimetrik.spring.account.depositservice.depositservice.service.impl;


import com.altimetrik.spring.account.depositservice.depositservice.model.Account;
import com.altimetrik.spring.account.depositservice.depositservice.model.Transaction;
import com.altimetrik.spring.account.depositservice.depositservice.queue.util.JmsQueueSender;
import com.altimetrik.spring.account.depositservice.depositservice.repository.IAccountRepository;
import com.altimetrik.spring.account.depositservice.depositservice.repository.ITransactionRepository;
import com.altimetrik.spring.account.depositservice.depositservice.service.ITransferService;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.altimetrik.spring.account.depositservice.depositservice.var.QueueName;
import com.github.loki4j.client.util.Loki4jLogger;

import org.slf4j.LoggerFactory;
import com.github.loki4j.slf4j.marker.LabelMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Date;

@Service
public class TransferService implements ITransferService{
    @Autowired
    private IAccountRepository accountRepository;

    @Autowired
    private ITransactionRepository transactionRepository;

    @Autowired
    private JmsQueueSender jmsQueueSender;
    
    private final Logger LOG = LoggerFactory.getLogger(AccountService.class);

    public void transferBalance(String from, String to, Double balance)
    {
        
        String transactionId=String.valueOf(new Date().getTime());
        Account accountFrom = accountRepository.getByName(from);
        Account accountTo = accountRepository.getByName(to);
        Transaction transaction = new Transaction(from, balance, "debit", to, new Date());
        Transaction transaction2 = new Transaction(to, balance, "credit", from, new Date());     
        if (accountFrom != null && accountFrom.getBalance() >= balance)
        {

            if (accountTo.getBalance() == null){
                accountFrom.setBalance(accountFrom.getBalance() - balance);
                accountTo.setBalance(balance);
                accountRepository.saveAll(Arrays.asList(accountFrom,accountTo));
                transactionRepository.save(transaction);
                transactionRepository.save(transaction2);
                LOG.info("Ammount "+balance+" recieved from "+ from+" successfully!");
                jmsQueueSender.sendMessage(QueueName.TRANSFER_ACK,getSuccessDepositAcknowledgement(transactionId));
            }
            else
            {
                accountFrom.setBalance(accountFrom.getBalance() - balance);
                accountTo.setBalance(accountTo.getBalance() + balance);
                accountRepository.saveAll(Arrays.asList(accountFrom,accountTo));
                transactionRepository.save(transaction);
                transactionRepository.save(transaction2);
                LOG.info("Ammount "+balance+" recieved from "+ from+" successfully!");
                jmsQueueSender.sendMessage(QueueName.TRANSFER_ACK,getSuccessDepositAcknowledgement(transactionId));
            }


        }
        else{
            LOG.error("Account not found or Invalid Balance");
            jmsQueueSender.sendMessage(QueueName.TRANSFER_ACK,getFailedDepositAcknowledgement(transactionId));
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