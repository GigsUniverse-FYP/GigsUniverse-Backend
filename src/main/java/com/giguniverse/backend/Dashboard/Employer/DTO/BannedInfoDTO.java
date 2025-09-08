package com.giguniverse.backend.Dashboard.Employer.DTO;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BannedInfoDTO {
    private String bannedReason;
    private LocalDateTime unbanDate;
    private long daysRemaining;

}
