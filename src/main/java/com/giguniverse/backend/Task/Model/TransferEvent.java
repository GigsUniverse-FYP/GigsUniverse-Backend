package com.giguniverse.backend.Task.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transfer_event")
public class TransferEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stripeEventId;       // evt_xxx
    private String stripeTransferId;    // tr_xxx
    private String balanceTransactionId; // txn_xxx (optional, can help for reconciliation)

    private String eventType;           // transfer.created, transfer.reversed, etc.

    private Long amount;                // in cents
    private String currency;            // e.g. "usd"

    private Boolean reversed;           // false if successful, true if reversed
    private Long amountReversed;        // in cents, 0 normally

    // Destination (freelancer's Stripe Express account)
    private String destinationAccountId;  // acct_xxx
    private String destinationPaymentId;  // py_xxx

    // Metadata
    private String description;         // "Transfer for Task XYZ"
    private Instant createdAt;          // event creation
    private Instant receivedAt;         // when webhook received/processed
    private String taskId;
    private String contractId;
    private String freelancerId;
    private String employerId;
}
