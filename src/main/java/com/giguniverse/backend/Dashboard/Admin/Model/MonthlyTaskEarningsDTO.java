package com.giguniverse.backend.Dashboard.Admin.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class MonthlyTaskEarningsDTO {
    private String month;          
    private double totalPay;       
    private double platformFee;  
}