package com.altimetrik.spring.account.depositservice.depositservice.queue.util;

import com.altimetrik.spring.account.depositservice.depositservice.service.IDepositService;

import com.altimetrik.spring.account.depositservice.depositservice.service.ITransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class JmsQueueListener {
    @Autowired
    private IDepositService depositService; 


    @Autowired
    private ITransferService transferService;

    @JmsListener(destination = "deposit", containerFactory = "myFactory")

    public void receiveMessageForDepositingBalance(HashMap<String, Object> depositMap)
    {
        System.out.println("Received Deposit <" + depositMap + ">");
        depositService.depositBalance((String)depositMap.get("name"),(double)depositMap.get("balance"));

    }
    @JmsListener(destination = "transfer", containerFactory = "myFactory")

    public void receiveMessageForTransferringBalance(HashMap<String, Object> transferMap)
    {
        System.out.println("Received Transfer <" + transferMap + ">");
        transferService.transferBalance(String.valueOf(transferMap.get("from")), String.valueOf(transferMap.get("to")), (double)transferMap.get("balance"));

    }

}
