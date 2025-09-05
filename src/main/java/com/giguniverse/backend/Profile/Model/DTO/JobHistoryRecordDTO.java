package com.giguniverse.backend.Profile.Model.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobHistoryRecordDTO {
    private String id;
    private String title;
    private String client;
    private String clientId;
    private String companyName;
    private String status;
    private String startDate;
    private String endDate;
    private int budget;
    private Double rating;
    private String feedback;
    private List<String> skills;
    private String cancellationReason;
}
