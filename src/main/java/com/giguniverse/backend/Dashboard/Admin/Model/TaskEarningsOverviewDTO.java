package com.giguniverse.backend.Dashboard.Admin.Model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class TaskEarningsOverviewDTO {
    private double totalEarnings;
    private double currentMonthEarnings;
    private double avgMonthly;
    private List<MonthlyTaskEarningsDTO> last6Months;
}

