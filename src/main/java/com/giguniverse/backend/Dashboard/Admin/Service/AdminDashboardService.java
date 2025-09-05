package com.giguniverse.backend.Dashboard.Admin.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.Auth.Repository.AdminRepository;
import com.giguniverse.backend.Auth.Repository.EmployerRepository;
import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.Dashboard.Admin.Model.MonthlyCompletedContractsDTO;
import com.giguniverse.backend.Dashboard.Admin.Model.MonthlyPayoutDTO;
import com.giguniverse.backend.Dashboard.Admin.Model.MonthlySubscriptionDTO;
import com.giguniverse.backend.Dashboard.Admin.Model.MonthlyTaskEarningsDTO;
import com.giguniverse.backend.Dashboard.Admin.Model.MonthlyUserRegistrationsDTO;
import com.giguniverse.backend.Dashboard.Admin.Model.TaskEarningsOverviewDTO;
import com.giguniverse.backend.JobPost.ContractHandling.Repository.ContractRepository;
import com.giguniverse.backend.Subscription.Repository.EmployerSubscriptionRepository;
import com.giguniverse.backend.Subscription.Repository.FreelancerSubscriptionRepository;
import com.giguniverse.backend.Task.Model.Task;
import com.giguniverse.backend.Task.Repository.TaskRepository;
import com.giguniverse.backend.Task.Repository.TransferEventRepository;

@Service
public class AdminDashboardService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private FreelancerRepository freelancerRepository;

    @Autowired
    private EmployerRepository employerRepository;
    
    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private TransferEventRepository transferEventRepository;

    @Autowired
    private EmployerSubscriptionRepository employerSubscriptionRepository;  

    @Autowired
    private FreelancerSubscriptionRepository freelancerSubscriptionRepository;

    public TaskEarningsOverviewDTO getAdminTaskEarningsOverview() {
        ZoneId utc = ZoneOffset.UTC;
        LocalDate now = LocalDate.now(utc);

        List<Task> allApproved = taskRepository.findByTaskStatus("approved");
        double totalEarnings = allApproved.stream()
                .mapToDouble(t -> t.getTaskTotalPay() != null ? t.getTaskTotalPay() : 0)
                .sum();

        Instant currentMonthStart = now.withDayOfMonth(1).atStartOfDay(utc).toInstant();
        Instant nextMonthStart = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(utc).toInstant();

        List<Task> currentMonthTasks = taskRepository.findByTaskStatusAndTaskSubmissionDateBetween(
                "approved", Date.from(currentMonthStart), Date.from(nextMonthStart)
        );
        double currentMonthEarnings = currentMonthTasks.stream()
                .mapToDouble(t -> t.getTaskTotalPay() != null ? t.getTaskTotalPay() : 0)
                .sum();

        final int N = 6;
        List<MonthlyTaskEarningsDTO> last6Months = new ArrayList<>(N);
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

        for (int i = N - 1; i >= 0; i--) {
            LocalDate targetMonth = now.minusMonths(i);

            Instant start = targetMonth.withDayOfMonth(1).atStartOfDay(utc).toInstant();
            Instant end = targetMonth.plusMonths(1).withDayOfMonth(1).atStartOfDay(utc).toInstant();

            List<Task> tasks = taskRepository.findByTaskStatusAndTaskSubmissionDateBetween(
                    "approved", Date.from(start), Date.from(end)
            );

            double monthEarnings = tasks.stream()
                    .mapToDouble(t -> t.getTaskTotalPay() != null ? t.getTaskTotalPay() : 0)
                    .sum();

            String monthLabel = targetMonth.format(monthFormatter);
            last6Months.add(MonthlyTaskEarningsDTO.builder()
                    .month(monthLabel)
                    .totalPay(monthEarnings)
                    .platformFee(monthEarnings * 0.08)
                    .build());
        }

        double avgMonthly = last6Months.stream()
                .mapToDouble(MonthlyTaskEarningsDTO::getTotalPay)
                .average()
                .orElse(0.0);

        return TaskEarningsOverviewDTO.builder()
                .totalEarnings(totalEarnings)
                .currentMonthEarnings(currentMonthEarnings)
                .avgMonthly(avgMonthly)
                .last6Months(last6Months)
                .build();
    }

    public List<Map<String, Object>> getUserRoleDistribution() {
        List<Map<String, Object>> data = new ArrayList<>();

        data.add(Map.of("name", "Freelancers", "value", freelancerRepository.count(), "color", "#000000"));
        data.add(Map.of("name", "Employers", "value", employerRepository.count(), "color", "#666666"));
        data.add(Map.of("name", "Admins", "value", adminRepository.count(), "color", "#d1d5db"));

        return data;
    }

    public List<MonthlyUserRegistrationsDTO> getLast6MonthsUserRegistrations() {
        ZoneId utc = ZoneOffset.UTC;
        LocalDate now = LocalDate.now(utc);

        final int N = 6;
        List<MonthlyUserRegistrationsDTO> result = new ArrayList<>(N);
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

        for (int i = N - 1; i >= 0; i--) {
            LocalDate targetMonth = now.minusMonths(i);

            LocalDateTime start = targetMonth.withDayOfMonth(1).atStartOfDay();
            LocalDateTime end = targetMonth.plusMonths(1).withDayOfMonth(1).atStartOfDay();

            long freelancers = freelancerRepository
                    .findByRegistrationDateBetween(start, end)
                    .size();

            long employers = employerRepository
                    .findByRegistrationDateBetween(start, end)
                    .size();

            result.add(MonthlyUserRegistrationsDTO.builder()
                    .month(targetMonth.format(monthFormatter))
                    .freelancers(freelancers)
                    .employers(employers)
                    .build());
        }

        return result;
    }


    public List<MonthlyCompletedContractsDTO> getLast6MonthsCompletedContracts() {
        ZoneId utc = ZoneOffset.UTC;
        LocalDate now = LocalDate.now(utc);

        final int N = 6;
        List<MonthlyCompletedContractsDTO> result = new ArrayList<>(N);
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

        for (int i = N - 1; i >= 0; i--) {
            LocalDate targetMonth = now.minusMonths(i);

            Instant start = targetMonth.withDayOfMonth(1).atStartOfDay(utc).toInstant();
            Instant end = targetMonth.plusMonths(1).withDayOfMonth(1).atStartOfDay(utc).toInstant();

            long completedCount = contractRepository
                    .findByContractStatusAndContractEndDateBetween("completed", Date.from(start), Date.from(end))
                    .size();

            result.add(MonthlyCompletedContractsDTO.builder()
                    .month(targetMonth.format(monthFormatter))
                    .completed(completedCount)
                    .build());
        }

        return result;
    }

    public List<MonthlyPayoutDTO> getLast6MonthsPayouts() {
        ZoneId utc = ZoneOffset.UTC;
        LocalDate now = LocalDate.now(utc);

        final int N = 6;
        List<MonthlyPayoutDTO> result = new ArrayList<>(N);
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

        for (int i = N - 1; i >= 0; i--) {
            LocalDate targetMonth = now.minusMonths(i);

            Instant start = targetMonth.withDayOfMonth(1).atStartOfDay(utc).toInstant();
            Instant end = targetMonth.plusMonths(1).withDayOfMonth(1).atStartOfDay(utc).toInstant();

            long totalCents = transferEventRepository
                    .findByReversedFalseAndCreatedAtBetween(start, end)
                    .stream()
                    .mapToLong(te -> te.getAmount() != null ? te.getAmount() : 0L)
                    .sum();

            long totalDollars = totalCents / 100;

            result.add(MonthlyPayoutDTO.builder()
                    .month(targetMonth.format(monthFormatter))
                    .payouts(totalDollars)
                    .build());
        }

        return result;
    }

    public List<MonthlySubscriptionDTO> getLast6MonthsSubscriptions() {
        ZoneId utc = ZoneOffset.UTC;
        LocalDate now = LocalDate.now(utc);

        final int N = 6;
        List<MonthlySubscriptionDTO> result = new ArrayList<>(N);
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

        for (int i = N - 1; i >= 0; i--) {
            LocalDate targetMonth = now.minusMonths(i);

            Instant start = targetMonth.withDayOfMonth(1).atStartOfDay(utc).toInstant();
            Instant end = targetMonth.plusMonths(1).withDayOfMonth(1).atStartOfDay(utc).toInstant();

            var employerSubs = employerSubscriptionRepository.findByCurrentPeriodStartBetween(start, end);
            long employerEarnings = employerSubs.stream()
                    .mapToLong(sub -> sub.getAmountPaid() != null ? sub.getAmountPaid() : 0L)
                    .sum();
            long employerCount = employerSubs.size();

            // Freelancers
            var freelancerSubs = freelancerSubscriptionRepository.findByCurrentPeriodStartBetween(start, end);
            long freelancerEarnings = freelancerSubs.stream()
                    .mapToLong(sub -> sub.getAmountPaid() != null ? sub.getAmountPaid() : 0L)
                    .sum();
            long freelancerCount = freelancerSubs.size();

            long totalEarnings = (employerEarnings + freelancerEarnings) / 100; // cents → dollars
            long totalSubscribers = employerCount + freelancerCount;

            result.add(MonthlySubscriptionDTO.builder()
                    .month(targetMonth.format(monthFormatter))
                    .earnings(totalEarnings)
                    .subscribers(totalSubscribers)
                    .build());
        }

        return result;
    }
}
