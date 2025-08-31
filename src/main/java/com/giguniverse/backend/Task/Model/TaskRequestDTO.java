package com.giguniverse.backend.Task.Model;

import java.util.Date;

import lombok.Data;

@Data
public class TaskRequestDTO {
    private String taskName;
    private String taskInstruction;
    private String taskSubmission;
    private String taskHour;
    private Date taskDueDate;
    private String employerId;
    private String freelancerId;
    private String jobId;
    private String contractId;
    private String taskTotalPay;  
}
