package com.giguniverse.backend.Dashboard.Transactions.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.Dashboard.Transactions.Model.SubscriptionDTO;
import com.giguniverse.backend.Subscription.Repository.EmployerSubscriptionRepository;
import com.giguniverse.backend.Subscription.Repository.FreelancerSubscriptionRepository;
import com.giguniverse.backend.Task.Model.TransferEvent;
import com.giguniverse.backend.Task.Repository.TransferEventRepository;
import com.giguniverse.backend.Transaction.Model.Transaction;
import com.giguniverse.backend.Transaction.Repository.TransactionRepository;

@Service
public class TransactionDisplayService {

    @Autowired
    FreelancerSubscriptionRepository freelancerSubscriptionRepository;
    @Autowired
    EmployerSubscriptionRepository employerSubscriptionRepository;
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    TransferEventRepository transferEventRepository;

    public List<SubscriptionDTO> getAllSubscriptions() {
        List<SubscriptionDTO> result = new ArrayList<>();

        freelancerSubscriptionRepository.findAll().forEach(f -> result.add(
            SubscriptionDTO.builder()
                .subscriptionId(String.valueOf(f.getFreelancerSubscriptionId()))
                .userId(f.getFreelancerUserId())
                .userType("freelancer")
                .stripeCustomerId(f.getStripeCustomerId())
                .stripeSubscriptionId(f.getStripeSubscriptionId())
                .stripeProductName(f.getStripeProductName())
                .status(f.getStatus())
                .currentPeriodStart(f.getCurrentPeriodStart())
                .currentPeriodEnd(f.getCurrentPeriodEnd())
                .amountPaid(f.getAmountPaid())
                .currency(f.getCurrency())
                .createdAt(f.getCreatedAt())
                .build()
        ));

        employerSubscriptionRepository.findAll().forEach(e -> result.add(
            SubscriptionDTO.builder()
                .subscriptionId(String.valueOf(e.getEmployerSubscriptionId()))
                .userId(e.getEmployerUserId())
                .userType("employer")
                .stripeCustomerId(e.getStripeCustomerId())
                .stripeSubscriptionId(e.getStripeSubscriptionId())
                .stripeProductName(e.getStripeProductName())
                .status(e.getStatus())
                .currentPeriodStart(e.getCurrentPeriodStart())
                .currentPeriodEnd(e.getCurrentPeriodEnd())
                .amountPaid(e.getAmountPaid())
                .currency(e.getCurrency())
                .createdAt(e.getCreatedAt())
                .build()
        ));

        result.sort(Comparator.comparing(SubscriptionDTO::getCreatedAt).reversed());
        return result;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .toList();
    }

    public List<TransferEvent> getAllEvents() {
        return transferEventRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(TransferEvent::getCreatedAt).reversed())
                .toList();
    }
}
