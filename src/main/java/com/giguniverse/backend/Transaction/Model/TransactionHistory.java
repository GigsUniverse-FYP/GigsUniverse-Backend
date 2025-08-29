package com.giguniverse.backend.Transaction.Model;

import lombok.Data;

@Data
public class TransactionHistory {
    private Long id;
    private String type; // paymentType
    private Long amount;
    private String currency;
    private String method; // paymentMethodType
    private String status;
    private String date; // createdAt
    private String description;
}
