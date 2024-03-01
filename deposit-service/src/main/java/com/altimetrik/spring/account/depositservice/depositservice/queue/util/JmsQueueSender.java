package com.altimetrik.spring.account.depositservice.depositservice.queue.util;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.jms.core.JmsTemplate;

@Component
public class JmsQueueSender {
    @Autowired
    private JmsTemplate jmsTemplate;

    public void sendMessage(String queueName, Object message)
    {
        this.jmsTemplate.convertAndSend(queueName,message);
    }
    
}