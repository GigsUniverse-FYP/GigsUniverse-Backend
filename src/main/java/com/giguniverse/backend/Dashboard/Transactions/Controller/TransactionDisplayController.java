package com.giguniverse.backend.Dashboard.Transactions.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.Dashboard.Transactions.Model.SubscriptionDTO;
import com.giguniverse.backend.Dashboard.Transactions.Service.TransactionDisplayService;
import com.giguniverse.backend.Task.Model.TransferEvent;
import com.giguniverse.backend.Transaction.Model.Transaction;

@RestController
@RequestMapping("/api/transactions-data")
public class TransactionDisplayController {

    @Autowired
    TransactionDisplayService transactionService;

    @GetMapping("/transactions")
    public List<Transaction> getAllTransactionEvents() {
        return transactionService.getAllTransactions();
    }

    @GetMapping("/transfer-events")
    public List<TransferEvent> getAllTransferEvents() {
        return transactionService.getAllEvents();
    }

    @GetMapping("/subscription-events")
    public List<SubscriptionDTO> getAllSubscriptionEvents() {
        return transactionService.getAllSubscriptions();
    }

}
