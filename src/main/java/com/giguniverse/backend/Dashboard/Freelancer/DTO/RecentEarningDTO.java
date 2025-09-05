package com.giguniverse.backend.Dashboard.Freelancer.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecentEarningDTO {
    private String action;
    private String project;
    private String amount;
    private String time;
}