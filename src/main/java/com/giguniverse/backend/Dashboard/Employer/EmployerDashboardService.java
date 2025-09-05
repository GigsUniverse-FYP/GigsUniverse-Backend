package com.giguniverse.backend.Dashboard.Employer;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Dashboard.Employer.DTO.EmployerDashboardDTO;
import com.giguniverse.backend.Dashboard.Employer.DTO.EmployerPayoutDTO;
import com.giguniverse.backend.Dashboard.Employer.DTO.EmployerPendingTaskDTO;
import com.giguniverse.backend.Dashboard.Employer.DTO.MonthlyPayoutDTO;
import com.giguniverse.backend.JobPost.ContractHandling.Model.Contract;
import com.giguniverse.backend.JobPost.ContractHandling.Repository.ContractRepository;
import com.giguniverse.backend.Profile.Model.FreelancerProfile;
import com.giguniverse.backend.Profile.Repository.FreelancerProfileRepository;
import com.giguniverse.backend.Task.Model.Task;
import com.giguniverse.backend.Task.Model.TransferEvent;
import com.giguniverse.backend.Task.Repository.TaskRepository;
import com.giguniverse.backend.Task.Repository.TransferEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmployerDashboardService {

    @Autowired
    private TransferEventRepository transferEventRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private FreelancerProfileRepository freelancerProfileRepository;

    public EmployerDashboardDTO getDashboardStats() {
        String employerId = AuthUtil.getUserId();
        if (employerId == null) throw new RuntimeException("User not authenticated");

        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.withDayOfMonth(1);
        LocalDate lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        Instant monthStart = firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant monthEnd = lastDayOfMonth.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();

        // Total payouts
        List<TransferEvent> transfers = transferEventRepository.findByEmployerIdAndReversedFalse(employerId);
        long totalPayout = transfers.stream().mapToLong(TransferEvent::getAmount).sum();
        long totalPayoutThisMonth = transfers.stream()
                .filter(t -> t.getCreatedAt().isAfter(monthStart) && t.getCreatedAt().isBefore(monthEnd))
                .mapToLong(TransferEvent::getAmount)
                .sum();

        // Contracts
        List<Contract> contracts = contractRepository.findByEmployerId(employerId);
        long completedContracts = contracts.stream()
                .filter(c -> "completed".equalsIgnoreCase(c.getContractStatus()))
                .count();
        long completedContractsThisMonth = contracts.stream()
                .filter(c -> "completed".equalsIgnoreCase(c.getContractStatus()) &&
                        c.getContractStartDate() != null &&
                        !c.getContractStartDate().before(Date.from(monthStart)) &&
                        !c.getContractStartDate().after(Date.from(monthEnd)))
                .count();
        long activeContracts = contracts.stream()
                .filter(c -> "active".equalsIgnoreCase(c.getContractStatus()))
                .count();
        long activeContractsThisMonth = contracts.stream()
                .filter(c -> "active".equalsIgnoreCase(c.getContractStatus()) &&
                        c.getContractStartDate() != null &&
                        !c.getContractStartDate().before(Date.from(monthStart)) &&
                        !c.getContractStartDate().after(Date.from(monthEnd)))
                .count();

        // Tasks
        List<Task> tasks = taskRepository.findByEmployerIdAndTaskStatusIn(
                employerId, List.of("pending", "submitted")
        );
        long totalActiveTasks = tasks.size();

        // Tasks due this week
        LocalDate weekStart = now.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = now.with(DayOfWeek.SUNDAY);
        Instant weekStartInstant = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant weekEndInstant = weekEnd.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();
        long totalActiveTasksOnDue = tasks.stream()
                .filter(t -> t.getTaskDueDate() != null &&
                        !t.getTaskDueDate().before(Date.from(weekStartInstant)) &&
                        !t.getTaskDueDate().after(Date.from(weekEndInstant)))
                .count();

        return new EmployerDashboardDTO(
                totalPayout,
                totalPayoutThisMonth,
                completedContracts,
                completedContractsThisMonth,
                activeContracts,
                activeContractsThisMonth,
                totalActiveTasks,
                totalActiveTasksOnDue
        );
    }


    public EmployerPayoutDTO getEmployerPayoutOverview() {
        String employerId = AuthUtil.getUserId();
        if (employerId == null) throw new RuntimeException("User not authenticated");

        ZoneId utc = ZoneOffset.UTC;
        LocalDate now = LocalDate.now(utc);

        // Total Payout (all transfers to freelancers from this employer)
        List<TransferEvent> allTransfers = transferEventRepository.findByEmployerIdAndReversedFalse(employerId);
        double totalPayout = allTransfers.stream()
                .mapToDouble(t -> t.getAmount() / 100.0)
                .sum();

        // Payout for current month
        Instant currentMonthStart = now.withDayOfMonth(1).atStartOfDay(utc).toInstant();
        Instant nextMonthStart = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(utc).toInstant();

        List<TransferEvent> monthTransfers = transferEventRepository
                .findByEmployerIdAndReversedFalseAndCreatedAtBetween(employerId, currentMonthStart, nextMonthStart);

        double currentMonthPayout = monthTransfers.stream()
                .mapToDouble(t -> t.getAmount() / 100.0)
                .sum();

        // Last 6 months breakdown
        final int N = 6;
        List<MonthlyPayoutDTO> last6Months = new ArrayList<>(N);
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

        for (int i = N - 1; i >= 0; i--) {
            LocalDate targetMonth = now.minusMonths(i);

            Instant start = targetMonth.withDayOfMonth(1).atStartOfDay(utc).toInstant();
            Instant end = targetMonth.plusMonths(1).withDayOfMonth(1).atStartOfDay(utc).toInstant();

            List<TransferEvent> transfers = transferEventRepository
                    .findByEmployerIdAndReversedFalseAndCreatedAtBetween(employerId, start, end);

            double monthPayout = transfers.stream()
                    .mapToDouble(t -> t.getAmount() / 100.0)
                    .sum();

            String monthLabel = targetMonth.format(monthFormatter);
            last6Months.add(new MonthlyPayoutDTO(monthLabel, monthPayout));
        }

        double avgMonthly = last6Months.stream()
                .mapToDouble(MonthlyPayoutDTO::getAmount)
                .average()
                .orElse(0.0);

        return EmployerPayoutDTO.builder()
                .totalPayout(totalPayout)
                .currentMonthPayout(currentMonthPayout)
                .avgMonthly(avgMonthly)
                .last6Months(last6Months)
                .build();
    }

    public List<EmployerPendingTaskDTO> getSubmittedTasksForEmployer() {
        String employerId = AuthUtil.getUserId();
        if (employerId == null) throw new RuntimeException("User not authenticated");

        List<Task> tasks = taskRepository.findByEmployerIdAndTaskStatus(employerId, "submitted");
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

        return tasks.stream().map(task -> {
            String freelancerName = freelancerProfileRepository
                    .findByFreelancer_FreelancerUserId(task.getFreelancerId())
                    .map(FreelancerProfile::getFullName)
                    .orElse("Unknown");

            return new EmployerPendingTaskDTO(
                    task.getTaskName(),
                    task.getFreelancerId(),
                    freelancerName,
                    task.getTaskDueDate() != null ? sdf.format(task.getTaskDueDate()) : null
            );
        }).collect(Collectors.toList());
    }

    public Map<String, Long> getContractStatusCount(String employerId) {
        List<Object[]> results = contractRepository.countEmployerContractsByStatus(employerId);

        Map<String, Long> statusCount = new HashMap<>();
        for (Object[] row : results) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            statusCount.put(status, count);
        }
        return statusCount;
    }
}
