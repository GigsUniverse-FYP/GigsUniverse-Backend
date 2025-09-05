package com.giguniverse.backend.Dashboard.Admin.Model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthlyCompletedContractsDTO {
    private String month;
    private long completed;
}