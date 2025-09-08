package com.giguniverse.backend.Dashboard.Freelancer;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Dashboard.Freelancer.DTO.BannedInfoDTO;
import com.giguniverse.backend.Dashboard.Freelancer.DTO.FreelancerDashboardDTO;
import com.giguniverse.backend.Dashboard.Freelancer.DTO.FreelancerEarningsDTO;
import com.giguniverse.backend.Dashboard.Freelancer.DTO.RecentEarningDTO;
import com.giguniverse.backend.Dashboard.Freelancer.DTO.SkillStatsDTO;
import com.giguniverse.backend.Dashboard.Freelancer.DTO.TaskDeadlineDTO;

@RestController
@RequestMapping("/api/freelancer/dashboard")
public class FreelancerDashboardController {

    private final FreelancerDashboardService dashboardService;

    public FreelancerDashboardController(FreelancerDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/upper-card")
    public FreelancerDashboardDTO getDashboard() {
        return dashboardService.getDashboard();
    }

    @GetMapping("/earnings-overview")
    public FreelancerEarningsDTO getEarningsOverview() {
        return dashboardService.getEarningsOverview();
    }

    @GetMapping("/top-skills")
    public List<SkillStatsDTO> getTopSkills() {
        return dashboardService.getTopSkills();
    }

    @GetMapping("/status-count")
    public Map<String, Long> getContractStatusCount() {
        String freelancerId = AuthUtil.getUserId(); 
        return dashboardService.getContractStatusCount(freelancerId);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<RecentEarningDTO>> getRecentEarnings() {
        return ResponseEntity.ok(dashboardService.getRecentEarnings());
    }

    @GetMapping("/upcoming-deadlines")
    public ResponseEntity<List<TaskDeadlineDTO>> getUpcomingDeadlines() {
        return ResponseEntity.ok(dashboardService.getUpcomingDeadlines());
    }

    @GetMapping("/banned-info")
    public ResponseEntity<BannedInfoDTO> getBannedInfo() {
   return dashboardService.getBannedInfo()
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NO_CONTENT)
            .body(new BannedInfoDTO("Account is not banned", null, 0)));
    }
}
