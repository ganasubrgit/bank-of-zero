package com.altimetrik.spring.account.depositservice.depositservice.model;

import org.springframework.lang.NonNull;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "account")
public class Account {

    @Id
    private String name;

    private Double balance;

    private double account;

    
    public double getAccount() {
        return account;
    }

    public void setAccount(double account) {
        this.account = account;
    }

    public Account()
    {

    }

    public Account(String name, double balance, double account)
    {
        this.name=name;
        this.balance=balance;
        this.account=account;

    }

    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    public Double getBalance()
    {
        return balance;
    }

    public void setBalance(Double balance)
    {
        this.balance = balance;
    }

    @Override
    public boolean equals(Object o)
    {
        if ( this == o) return true;
        if ( o == null || getClass() != o.getClass()) return false;

        Account account = (Account) o;
        return Objects.equals(name, account.name) && Objects.equals(balance, account.balance);


    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, balance);
    }

    @Override
    public String toString()
    {
        return "Account {" +
                "name='" +name + '\'' +
                ", balance=" + balance +
                '}';
    }
    
}
