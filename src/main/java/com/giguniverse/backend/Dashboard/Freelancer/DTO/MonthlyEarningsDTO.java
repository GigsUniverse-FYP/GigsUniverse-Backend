package com.giguniverse.backend.Dashboard.Freelancer.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyEarningsDTO {
    private String month;
    private double amount;
}

