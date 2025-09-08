package com.giguniverse.backend.Dashboard.Employer;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Dashboard.Employer.DTO.EmployerDashboardDTO;
import com.giguniverse.backend.Dashboard.Employer.DTO.EmployerPayoutDTO;
import com.giguniverse.backend.Dashboard.Employer.DTO.EmployerPendingTaskDTO;
import com.giguniverse.backend.Dashboard.Employer.DTO.BannedInfoDTO;

@RestController
@RequestMapping("/api/dashboard/employer")
public class EmployerDashboardController {

    @Autowired
    private EmployerDashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<EmployerDashboardDTO> getEmployerDashboard() {
        EmployerDashboardDTO stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }


    @GetMapping("/payout-overview")
    public ResponseEntity<EmployerPayoutDTO> getPayoutOverview() {
        EmployerPayoutDTO dto = dashboardService.getEmployerPayoutOverview();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/submitted-tasks")
    public ResponseEntity<List<EmployerPendingTaskDTO>> getSubmittedTasks() {
        List<EmployerPendingTaskDTO> tasks = dashboardService.getSubmittedTasksForEmployer();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/status-count")
    public Map<String, Long> getContractStatusCount() {
        String employerId = AuthUtil.getUserId();
        return dashboardService.getContractStatusCount(employerId);
    }

    @GetMapping("/banned-info")
    public ResponseEntity<BannedInfoDTO> getBannedInfo() {
   return dashboardService.getBannedInfo()
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NO_CONTENT)
            .body(new BannedInfoDTO("Account is not banned", null, 0)));
    }

}
