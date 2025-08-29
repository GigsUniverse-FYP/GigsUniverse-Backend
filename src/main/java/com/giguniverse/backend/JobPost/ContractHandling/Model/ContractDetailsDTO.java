package com.giguniverse.backend.JobPost.ContractHandling.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractDetailsDTO {
    private Integer jobId;
    private String jobName;
    private String employerId;
    private String employerName;
    private String freelancerId;
    private String freelancerName;
}

