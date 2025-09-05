package com.giguniverse.backend.Dashboard.Employer.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployerDashboardDTO {
    private long totalPayout; // in cents
    private long totalPayoutThisMonth; // in cents
    private long completedContracts; 
    private long completedContractsThisMonth;
    private long activeContracts;
    private long activeContractsThisMonth;
    private long totalActiveTasks; 
    private long totalActiveTasksOnDue; // pending/submitted tasks due this week
}
