package com.giguniverse.backend.Dashboard.Admin.Model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthlyPayoutDTO {
    private String month;
    private Long payouts;
}