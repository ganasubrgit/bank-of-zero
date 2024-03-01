package com.altimetrik.spring.account.depositservice.depositservice.service;

public interface  ITransferService {
    void transferBalance(String from, String to, Double balance);
}
