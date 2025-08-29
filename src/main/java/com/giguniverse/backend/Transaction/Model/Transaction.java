package com.giguniverse.backend.Transaction.Model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions")
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String employerUserId; // who top up

    private String stripePaymentIntentId; // stripe connection id

    private String stripeCheckoutSessionId; // stripe checkout session id

    private Long amount; // amount top up (in cents) (- for payment)

    private String currency; // currency top up

    private String status; // success, failed, pending

    private String paymentMethodType; // card, bank_transfer, etc.

    private String paymentType; // "top up", "refund"

    private String description; // "employer top up", "escrow payload", "refund from project"

    private Instant createdAt; // creation timestamp

    private Instant updatedAt; // last update timestamp
}
