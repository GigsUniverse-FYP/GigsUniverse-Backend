package com.giguniverse.backend.Dashboard.Employer.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyPayoutDTO {
    private String month;
    private double amount; 
}
