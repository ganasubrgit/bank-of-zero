package com.altimetrik.spring.account.depositservice.depositservice.repository;

import com.altimetrik.spring.account.depositservice.depositservice.model.Account;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IAccountRepository extends CrudRepository<Account, String>{

    Boolean existsAccountByName(String name);
    @Query("select a from Account a")
    List<Account> getAll();
    Account getByName(String name);
    Account getAccountByName(String name);
    
}
