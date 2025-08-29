package com.giguniverse.backend.JobPost.ContractHandling.Model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.util.Date;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Builder
@Entity
@Table(name = "contract")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int contractId;

    private int agreedPayRatePerHour; // in $

    private String contractStatus; // "rejected (freelancer reject contract)", "pending (wait for reply)" "upcoming", "active", "completed", "cancelled (both freelancer/employer wish to cancel earlier)"

    @Nullable
    private String cancellationReason; // if cancel early by freelancer / employer -> request

    @Nullable
    private String approveEarlyCancellation; // "approved", "rejected"

    private String hourPerWeek; // setted hour per week

    private Date contractCreationDate; // created date

    private Date contractStartDate; // start date

    private Date contractEndDate; // end date

    private Boolean freelancerFeedback; // freelancer need to give feedback or else will block other jobs

    private String jobApplicationId; // checking back

    private String jobId; // for job

    private String employerId; // for employer

    private String freelancerId; // for freelancer
}
