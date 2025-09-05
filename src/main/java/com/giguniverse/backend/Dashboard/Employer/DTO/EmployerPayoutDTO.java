package com.giguniverse.backend.Dashboard.Employer.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployerPayoutDTO {
    private double totalPayout;             // total payout in USD
    private double currentMonthPayout;      // payout in current month
    private double avgMonthly;              // average of last 6 months
    private List<MonthlyPayoutDTO> last6Months;
}