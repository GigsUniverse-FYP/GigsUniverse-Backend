package com.giguniverse.backend.Transaction.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.Transaction.Model.TopUpRequest;
import com.giguniverse.backend.Transaction.Model.TransactionHistory;
import com.giguniverse.backend.Transaction.Service.TransactionService;
import com.stripe.exception.StripeException;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    TransactionService transactionService;

    @GetMapping("/available-credits")
    public Long getAvailableCredits() {
        return transactionService.getAvailableCreditsForCurrentEmployer();
    }

    @PostMapping("/topup")
    public ResponseEntity<Map<String, Object>> topUp(@RequestBody TopUpRequest request) throws StripeException {
        String checkoutUrl = transactionService.createStripeTopUpSession(request);
        return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
    }

    
    @GetMapping("/history")
    public ResponseEntity<List<TransactionHistory>> getTransactionHistory() {
        List<TransactionHistory> history = transactionService.getMyTransactionHistory();
        return ResponseEntity.ok(history);
    }
}
