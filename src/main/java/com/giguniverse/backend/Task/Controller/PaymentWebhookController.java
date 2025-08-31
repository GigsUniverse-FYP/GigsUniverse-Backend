package com.giguniverse.backend.Task.Controller;

import com.giguniverse.backend.Task.Model.Task;
import com.giguniverse.backend.Task.Model.TransferEvent;
import com.giguniverse.backend.Task.Repository.TaskRepository;
import com.giguniverse.backend.Task.Repository.TransferEventRepository;
import com.giguniverse.backend.Transaction.Model.Transaction;
import com.giguniverse.backend.Transaction.Repository.TransactionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Transfer;
import com.stripe.net.Webhook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/stripe/webhook")
public class PaymentWebhookController {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TransferEventRepository transferEventRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Value("${stripe.webhook.pay.express.secret}")
    private String endpointSecret;

   @PostMapping("/make-payment")
    public ResponseEntity<String> handlePaymentWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            System.out.println("Webhook verified.");
            System.out.println("Event ID: " + event.getId());
            System.out.println("Event Type: " + event.getType());
            System.out.println("Event Payload: " + payload);
        } catch (Exception e) {
            System.out.println("Invalid webhook signature: " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid webhook signature");
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Transfer transfer;

        try {
            if (deserializer.getObject().isPresent() && deserializer.getObject().get() instanceof Transfer t) {
                transfer = t;
                System.out.println("Transfer object deserialized from event.");
            } else {
                System.out.println("Deserializer empty, attempting manual retrieve");
                // Extract the transfer ID from the event data
                String transferId = ((Transfer) event.getData().getObject()).getId();
                transfer = Transfer.retrieve(transferId);
                System.out.println("Transfer retrieved manually with ID: " + transferId);
            }
        } catch (StripeException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to retrieve transfer object");
        }

        System.out.println("Stripe Transfer ID: " + transfer.getId());
        System.out.println("Metadata: " + transfer.getMetadata());
        System.out.println("Amount: " + transfer.getAmount());
        System.out.println("Currency: " + transfer.getCurrency());
        System.out.println("Reversed: " + transfer.getReversed());
        System.out.println("BalanceTx ID: " + transfer.getBalanceTransaction());

        String taskId = transfer.getMetadata().get("taskId");
        if (taskId == null) {
            System.out.println("No taskId in transfer metadata. Metadata=" + transfer.getMetadata());
            return ResponseEntity.ok("No taskId, skipping");
        }

        System.out.println("Found taskId in metadata: " + taskId);

        // Find existing TransferEvent if any
        Optional<TransferEvent> existingEventOpt = transferEventRepository.findByStripeTransferId(transfer.getId());
        System.out.println("Existing TransferEvent? " + existingEventOpt.isPresent());

        switch (event.getType()) {
            case "transfer.created" -> {
                System.out.println("Handling transfer.created for taskId=" + taskId);

                TransferEvent te = existingEventOpt.orElseGet(TransferEvent::new);
                te.setStripeEventId(event.getId());
                te.setStripeTransferId(transfer.getId());
                te.setBalanceTransactionId(transfer.getBalanceTransaction());
                te.setAmount(transfer.getAmount());
                te.setCurrency(transfer.getCurrency());
                te.setDestinationAccountId(transfer.getDestination());
                te.setDestinationPaymentId(transfer.getDestinationPayment());
                te.setEventType("transfer.created");
                te.setReversed(transfer.getReversed());
                te.setAmountReversed(transfer.getAmountReversed());
                te.setTaskId(taskId);
                te.setContractId(transfer.getMetadata().get("contractId"));
                te.setFreelancerId(transfer.getMetadata().get("freelancerId"));
                te.setEmployerId(transfer.getMetadata().get("employerId"));
                te.setDescription(transfer.getDescription());
                te.setCreatedAt(Instant.ofEpochSecond(transfer.getCreated()));
                te.setReceivedAt(Instant.now());

                transferEventRepository.save(te);
                System.out.println("TransferEvent saved for taskId=" + taskId);

                if (!transfer.getReversed()) {
                    System.out.println("Looking up Task with ID=" + taskId);
                    Task task = taskRepository.findById(Integer.parseInt(taskId))
                            .orElseThrow(() -> new RuntimeException("❌ Task not found for ID=" + taskId));
                    System.out.println("Found Task. Current status=" + task.getTaskStatus());
                    task.setTaskStatus("approved");
                    taskRepository.save(task);
                    System.out.println("Task " + taskId + " marked as APPROVED");

                    Transaction transaction = Transaction.builder()
                        .employerUserId(transfer.getMetadata().get("employerId")) 
                        .stripePaymentIntentId(null)          
                        .stripeCheckoutSessionId(null)       
                        .amount(-transfer.getAmount()) 
                        .currency(transfer.getCurrency())
                        .status("success")
                        .paymentMethodType("credits")
                        .paymentType("Release Task Payment")
                        .description(transfer.getDescription())
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();

                    transactionRepository.save(transaction);
                }
            }

            case "transfer.reversed" -> {
                System.out.println("Handling transfer.reversed for taskId=" + taskId);

                TransferEvent te = existingEventOpt.orElseThrow(() ->
                        new RuntimeException("TransferEvent not found for reversed transfer"));
                te.setEventType("transfer.reversed");
                te.setReversed(true);
                te.setAmountReversed(transfer.getAmountReversed());
                te.setReceivedAt(Instant.now());
                transferEventRepository.save(te);
                System.out.println("TransferEvent updated to reversed for taskId=" + taskId);

                Task task = taskRepository.findById(Integer.parseInt(taskId))
                        .orElseThrow(() -> new RuntimeException("Task not found for ID=" + taskId));
                System.out.println("Found Task. Current status=" + task.getTaskStatus());
                task.setTaskStatus("submitted");
                taskRepository.save(task);
                System.out.println("Task " + taskId + " marked as PENDING due to reversed transfer");

                Transaction transaction = Transaction.builder()
                    .employerUserId(transfer.getMetadata().get("employerId")) 
                    .stripePaymentIntentId(null)          
                    .stripeCheckoutSessionId(null)       
                    .amount(-transfer.getAmount()) 
                    .currency(transfer.getCurrency())
                    .status("failed")
                    .paymentMethodType("credits")
                    .paymentType("Release Task Payment")
                    .description(transfer.getDescription())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

                    transactionRepository.save(transaction);
            }

            default -> {
                System.out.println("Unhandled event type: " + event.getType());
                System.out.println("Event data: " + payload);
            }
        }

        return ResponseEntity.ok("Webhook processed");
    }

}