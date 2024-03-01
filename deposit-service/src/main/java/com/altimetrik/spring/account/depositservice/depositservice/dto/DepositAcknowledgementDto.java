package com.altimetrik.spring.account.depositservice.depositservice.dto;


import java.util.Objects;

import com.altimetrik.spring.account.depositservice.depositservice.model.Account;


public class DepositAcknowledgementDto {
    private String transactionId;
    private String status;
    private String message;

    public DepositAcknowledgementDto()
    {

    }
    public DepositAcknowledgementDto(String transactionId, String status, String message)
    {
        this.transactionId = transactionId;
        this.message = message;
        this.status = status;
    }

    public String getTransactionId()
    {
        return transactionId;
    }
    public void setTransactionId(String transactionId)
    {
        this.transactionId = transactionId;
    }
    public String getMessage()
    {
        return message;
    }
    public void setMessage(String message)
    {
        this.message = message;
    }
    public String getStatus()
    {
        return status;
    }
    public void setStatus(String status)
    {
        this.status = status;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if ( this == o) return true;
        if ( o == null || getClass() != o.getClass()) return false;

        DepositAcknowledgementDto that = (DepositAcknowledgementDto) o;
        return Objects.equals(transactionId,that.transactionId);


    }
    @Override
    public int hashCode()
    {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString()
    {
        return "DepositAcknowledgementDto {" +
                "transactionId='" +transactionId + '\'' +
                ", status=" + status +
                ", message=" + message +
                '}';
    }
    
}
