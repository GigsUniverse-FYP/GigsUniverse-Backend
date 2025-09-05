package com.giguniverse.backend.JobPost.ContractHandling.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractAdminDetailsDTO {
    private int contractId;
    private int agreedPayRatePerHour;
    private String contractStatus;
    private String cancellationReason;
    private String approveEarlyCancellation;
    private String hourPerWeek;
    private Date contractCreationDate;
    private Date contractStartDate;
    private Date contractEndDate;
    private Boolean freelancerFeedback;
    private String jobId;
    private String jobTitle;
    private String companyName;
    private String employerId;
    private String employerName;
    private String employerEmail;
    private String employerCompany;
    private String freelancerId;
    private String freelancerName;
    private String freelancerEmail;
}