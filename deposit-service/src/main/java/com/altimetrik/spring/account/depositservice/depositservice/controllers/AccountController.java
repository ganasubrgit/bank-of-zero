package com.altimetrik.spring.account.depositservice.depositservice.controllers;

import com.altimetrik.spring.account.depositservice.depositservice.model.Account;
import com.altimetrik.spring.account.depositservice.depositservice.model.Transaction;
import com.altimetrik.spring.account.depositservice.depositservice.service.IAccountService;
import com.altimetrik.spring.account.depositservice.depositservice.service.IDepositService;
import com.altimetrik.spring.account.depositservice.depositservice.service.ITransferService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RestController
@RequestMapping("account")
public class AccountController {
    
    @Autowired
    private IAccountService accountService;

    @Autowired
    private ITransferService transferService;

    @Autowired
    private IDepositService depositService2;


    @PostMapping("create")
    public Account create(@RequestBody /*@Validated */ Account account)
    {
        return accountService.create(account);
    }

    @PutMapping("update")
    public Account update(@RequestBody @Validated Account account)
    {
        return accountService.update(account);
    }

    @GetMapping("get-all")
    public List<Account> getAll()
    {
        return accountService.getAll();
    }

    @GetMapping("exists")
    public Boolean existsAccountByName(@RequestParam String name)
    {
        return accountService.existsAccountByName(name);
    }

    @GetMapping("fetch-balance")
    public Account fetchAccountBalance(@RequestParam String name)
    {
        return accountService.getByName(name);
    }

    @GetMapping("get-transactions")
    public List<Transaction> getTransactions()
    {
        return accountService.getTransactions();
    }

    @GetMapping("get-transactions-by-name")
    public List<Transaction> getTransactionByName(String name, String recipient)
    {
        return accountService.getTransactionByName(name, recipient);
    }

    @PostMapping("transfer")
    public  void transferBalance(String from, String to, Double balance)
    {
         transferService.transferBalance( from,  to,  balance);
    }

    @PostMapping("deposit")
    public  void depositBalance(String name, Double balance)
    {
         depositService2.depositBalance( name, balance);
    }


}
