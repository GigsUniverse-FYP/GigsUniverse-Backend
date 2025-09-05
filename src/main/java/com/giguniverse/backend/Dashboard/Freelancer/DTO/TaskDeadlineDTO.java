package com.giguniverse.backend.Dashboard.Freelancer.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskDeadlineDTO {
    private String project;
    private String client;  
    private String deadline;
    private String status;
}