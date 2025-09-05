package com.giguniverse.backend.Company.Model;

import lombok.Data;

@Data
public class StatusUpdateRequest {
    private String newStatus;
    private String reason;
}

