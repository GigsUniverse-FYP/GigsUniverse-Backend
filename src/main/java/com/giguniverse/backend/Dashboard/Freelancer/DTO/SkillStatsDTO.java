package com.giguniverse.backend.Dashboard.Freelancer.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SkillStatsDTO {
    private String skill;
    private double averageRating;
    private int projectsCompleted;
}
