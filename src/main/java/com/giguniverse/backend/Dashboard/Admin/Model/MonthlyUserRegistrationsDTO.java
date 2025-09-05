package com.giguniverse.backend.Dashboard.Admin.Model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthlyUserRegistrationsDTO {
    private String month;
    private long freelancers;
    private long employers;
}