package com.altimetrik.spring.account.depositservice.depositservice.repository;


import com.altimetrik.spring.account.depositservice.depositservice.model.Transaction;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.lang.String;

@Repository
public interface ITransactionRepository extends CrudRepository<Transaction, String> {
    Boolean existsAccountByName(String name);
    @Query("select a from Transaction a")
    List<Transaction> getTransactions();
    @Query("SELECT u FROM Transaction u WHERE u.name = ?1 or u.recipient = ?2")
    List<Transaction> getTransactionByName(String name , String recipient);
    
}
