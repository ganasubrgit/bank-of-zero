package com.altimetrik.spring.account.depositservice.depositservice.model;
import org.springframework.lang.NonNull;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Size;

import jakarta.persistence.Table;

import java.util.Date;
import java.util.Objects;


@Entity
@IdClass(Transaction.class)
@Table(name = "transaction")
public class Transaction {

    @Column(length = 10)
    private Double ammount;
    @Column(length = 10)
    private String tranType; //credit or debit
    @Column(length = 100)
    private String recipient; //recipient
    @Id
    @Column(length = 100)
    private String name;
    @JsonFormat(pattern="yyyy-MM-dd")
    @Id
    private Date createdOn;

    public Date getCreatedOn() {
        return createdOn;
    }
    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }
    public Transaction()
    {

    }
    public Transaction(String name, Double ammount, String tranType, String recipient, Date createdOn)
    {
        this.ammount=ammount;
        this.name=name;
        this.recipient=recipient;
        this.tranType=tranType;
        this.createdOn = createdOn;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Double getAmmount() {
        return ammount;
    }
    public void setAmmount(Double ammount) {
        this.ammount = ammount;
    }
    public String getTranType() {
        return tranType;
    }
    public void setTranType(String tranType) {
        this.tranType = tranType;
    }
    public String getRecipient() {
        return recipient;
    }
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
    
    @Override
    public String toString() {
        return "Transaction [name=" + name + ", ammount=" + ammount + ", tranType=" + tranType + ", recipient="
                + recipient + "]";
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((ammount == null) ? 0 : ammount.hashCode());
        result = prime * result + ((tranType == null) ? 0 : tranType.hashCode());
        result = prime * result + ((recipient == null) ? 0 : recipient.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Transaction other = (Transaction) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (ammount == null) {
            if (other.ammount != null)
                return false;
        } else if (!ammount.equals(other.ammount))
            return false;
        if (tranType == null) {
            if (other.tranType != null)
                return false;
        } else if (!tranType.equals(other.tranType))
            return false;
        if (recipient == null) {
            if (other.recipient != null)
                return false;
        } else if (!recipient.equals(other.recipient))
            return false;
        return true;
    }



}
