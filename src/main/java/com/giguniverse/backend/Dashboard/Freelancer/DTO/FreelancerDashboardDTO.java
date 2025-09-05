package com.giguniverse.backend.Dashboard.Freelancer.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class FreelancerDashboardDTO {
    private double totalEarnings;
    private double currentMonthEarnings;

    private long totalCompletedContracts;
    private long currentMonthCompletedContracts;   

    private long totalActiveProjects;               
    private long activeProjectsThisWeek;            

    private double successRate;                     
}
