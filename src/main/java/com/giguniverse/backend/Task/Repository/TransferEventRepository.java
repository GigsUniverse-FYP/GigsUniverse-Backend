package com.giguniverse.backend.Task.Repository;

import com.giguniverse.backend.Task.Model.TransferEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransferEventRepository extends JpaRepository<TransferEvent, Long> {

    List<TransferEvent> findByTaskId(String taskId);

    List<TransferEvent> findByFreelancerId(String freelancerId);

    Optional<TransferEvent> findByStripeTransferId(String stripeTransferId);

    List<TransferEvent> findByFreelancerIdAndReversedFalse(String freelancerId);
    
    List<TransferEvent> findByFreelancerIdAndReversedFalseAndCreatedAtBetween(
            String freelancerId, Instant start, Instant end
    );

    List<TransferEvent> findByFreelancerIdOrderByCreatedAtDesc(String freelancerId);

    List<TransferEvent> findByEmployerIdAndReversedFalse(String employerId);

    List<TransferEvent> findByEmployerIdAndReversedFalseAndCreatedAtBetween(
            String employerId, Instant start, Instant end
    );

    List<TransferEvent> findByReversedFalseAndCreatedAtBetween(Instant start, Instant end);
}

