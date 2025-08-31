package com.giguniverse.backend.Task.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.util.Date;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Builder
@Entity
@Table(name = "task")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int taskId;

    private String taskName;

    @Column(columnDefinition = "TEXT")
    private String taskInstruction;

    @Column(columnDefinition = "TEXT")
    private String taskSubmission; // allow them to add link for specify what file to upload etc.

    @Column(columnDefinition = "TEXT")
    private String taskComment; // 

    private String taskStatus; // pending, submitted, approved, rejected

    @Nullable
    private String taskSubmissionNote;

    @Nullable
    private String rejectReason; // if rejected, give reason

    private String taskHour;  // determine the total pay

    private Long taskTotalPay;  // based on agreed hourly rate * hours needed in contract 

    private Date taskCreationDate;

    private Date taskSubmissionDate;

    private Date taskDueDate;

    // linking other entities
    private String employerId;

    private String freelancerId;

    private String jobId;

    private String contractId;

}
