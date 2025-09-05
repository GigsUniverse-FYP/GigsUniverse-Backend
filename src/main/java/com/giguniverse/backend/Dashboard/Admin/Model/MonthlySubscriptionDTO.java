package com.giguniverse.backend.Dashboard.Admin.Model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthlySubscriptionDTO {
    private String month;
    private Long earnings;     // in dollars
    private Long subscribers;  // active count
}