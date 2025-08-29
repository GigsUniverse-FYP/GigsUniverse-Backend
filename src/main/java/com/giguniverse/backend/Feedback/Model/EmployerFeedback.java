package com.giguniverse.backend.Feedback.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import jakarta.persistence.*;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Builder
@Entity
@Table(name = "feedback_employer")
public class EmployerFeedback { // given by employer to freelancer
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int employerFeedbackId;

    private int rating; // 1 to 5
    private String feedback; // texts

    private String employerId;
    private String freelancerId;
    private int jobId;
    private int contractId;

}
