package com.giguniverse.backend.Dashboard.Freelancer.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FreelancerEarningsDTO {
    private double totalEarnings;
    private double currentMonthEarnings;
    private double avgMonthly;
    private List<MonthlyEarningsDTO> last6Months;

}


 

