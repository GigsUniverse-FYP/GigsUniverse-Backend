package com.giguniverse.backend.Dashboard.Admin.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.Dashboard.Admin.Model.MonthlyCompletedContractsDTO;
import com.giguniverse.backend.Dashboard.Admin.Model.MonthlyPayoutDTO;
import com.giguniverse.backend.Dashboard.Admin.Model.MonthlySubscriptionDTO;
import com.giguniverse.backend.Dashboard.Admin.Model.MonthlyUserRegistrationsDTO;
import com.giguniverse.backend.Dashboard.Admin.Model.TaskEarningsOverviewDTO;
import com.giguniverse.backend.Dashboard.Admin.Service.AdminDashboardService;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @Autowired
    private AdminDashboardService adminDashboardService;


    // task earnings
    @GetMapping("/earnings-overview")
    public TaskEarningsOverviewDTO getAdminTaskEarningsOverview() {
        return adminDashboardService.getAdminTaskEarningsOverview();
    }

    // role distribution
    @GetMapping("/roles")
    public List<Map<String, Object>> getRoleDistribution() {
        return adminDashboardService.getUserRoleDistribution();
    }

    // new user joining
    @GetMapping("/user-growth")
    public List<MonthlyUserRegistrationsDTO> getUserGrowth() {
        return adminDashboardService.getLast6MonthsUserRegistrations();
    }

    // completed contracts
    @GetMapping("/contracts/completed")
    public List<MonthlyCompletedContractsDTO> getCompletedContractsLast6Months() {
        return adminDashboardService.getLast6MonthsCompletedContracts();
    }

    // total payouts
    @GetMapping("/payouts")
    public List<MonthlyPayoutDTO> getLast6MonthsPayouts() {
        return adminDashboardService.getLast6MonthsPayouts();
    }

    // total subscriptions
    @GetMapping("/subscriptions")
    public List<MonthlySubscriptionDTO> getLast6MonthsSubscriptions() {
        return adminDashboardService.getLast6MonthsSubscriptions();
    }
}
