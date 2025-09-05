package com.giguniverse.backend.Dashboard.Employer.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployerPendingTaskDTO {
    private String taskName;
    private String freelancerId;
    private String freelancerName;
    private String dueDate; // formatted
}