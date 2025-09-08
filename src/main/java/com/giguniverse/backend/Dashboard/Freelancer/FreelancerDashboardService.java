package com.giguniverse.backend.Dashboard.Freelancer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.Dashboard.Freelancer.DTO.BannedInfoDTO;
import com.giguniverse.backend.Dashboard.Freelancer.DTO.FreelancerDashboardDTO;
import com.giguniverse.backend.Dashboard.Freelancer.DTO.FreelancerEarningsDTO;
import com.giguniverse.backend.Dashboard.Freelancer.DTO.MonthlyEarningsDTO;
import com.giguniverse.backend.Dashboard.Freelancer.DTO.RecentEarningDTO;
import com.giguniverse.backend.Dashboard.Freelancer.DTO.SkillStatsDTO;
import com.giguniverse.backend.Dashboard.Freelancer.DTO.TaskDeadlineDTO;
import com.giguniverse.backend.Feedback.Model.FreelancerFeedback;
import com.giguniverse.backend.Feedback.Repository.FreelancerFeedbackRepository;
import com.giguniverse.backend.JobPost.ContractHandling.Model.Contract;
import com.giguniverse.backend.JobPost.ContractHandling.Repository.ContractRepository;
import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPost;
import com.giguniverse.backend.JobPost.CreateJobPost.repository.JobPostRepository;
import com.giguniverse.backend.Profile.Model.EmployerProfile;
import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;
import com.giguniverse.backend.Task.Model.Task;
import com.giguniverse.backend.Task.Model.TransferEvent;
import com.giguniverse.backend.Task.Repository.TaskRepository;
import com.giguniverse.backend.Task.Repository.TransferEventRepository;

import lombok.RequiredArgsConstructor;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FreelancerDashboardService {

    @Autowired
    private TransferEventRepository transferEventRepo;
    @Autowired
    private ContractRepository contractRepo;
    @Autowired
    private TaskRepository taskRepo;
    @Autowired
    private JobPostRepository jobPostRepo;
    @Autowired
    private FreelancerFeedbackRepository freelancerFeedbackRepo;
    @Autowired
    private EmployerProfileRepository employerProfileRepo;
    @Autowired
    private FreelancerRepository freelancerRepository;

    public FreelancerDashboardDTO getDashboard() {
        String freelancerId = com.giguniverse.backend.Auth.Session.AuthUtil.getUserId();
        if (freelancerId == null) throw new RuntimeException("User not authenticated");

        List<TransferEvent> allTransfers = transferEventRepo.findByFreelancerIdAndReversedFalse(freelancerId);
        double totalEarnings = allTransfers.stream().mapToDouble(t -> t.getAmount() / 100.0).sum();

        LocalDate now = LocalDate.now();
        Instant monthStart = now.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant nextMonthStart = now.plusMonths(1).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<TransferEvent> monthTransfers =
                transferEventRepo.findByFreelancerIdAndReversedFalseAndCreatedAtBetween(
                        freelancerId, monthStart, nextMonthStart
                );
        double currentMonthEarnings = monthTransfers.stream().mapToDouble(t -> t.getAmount() / 100.0).sum();

        Date monthStartDate = Date.from(monthStart);
        Date nextMonthStartDate = Date.from(nextMonthStart);

        long totalCompletedContracts =
                contractRepo.countByFreelancerIdAndContractStatusAndContractEndDateIsNotNull(
                        freelancerId, "completed"
                );

        long currentMonthCompletedContracts =
                contractRepo.countByFreelancerIdAndContractStatusAndContractEndDateBetween(
                        freelancerId, "completed", monthStartDate, nextMonthStartDate
                );

        long totalActiveProjects = taskRepo.countByFreelancerIdAndTaskStatusIn(
                freelancerId, List.of("pending", "submitted")
        );

        LocalDate weekStart = now;
        LocalDate weekEnd = now.plusDays(7);
        Date weekStartDate = Date.from(weekStart.atStartOfDay().toInstant(ZoneOffset.UTC));
        Date weekEndDate = Date.from(weekEnd.atStartOfDay().toInstant(ZoneOffset.UTC));

        long activeProjectsThisWeek = taskRepo
                .findByFreelancerIdAndTaskStatusInAndTaskDueDateBetween(
                        freelancerId, List.of("pending", "submitted"), weekStartDate, weekEndDate
                ).size();

        long approved = taskRepo.countByFreelancerIdAndTaskStatus(freelancerId, "approved");
        long rejected = taskRepo.countByFreelancerIdAndTaskStatus(freelancerId, "rejected");
        double successRate = (approved + rejected) > 0 ? (approved * 100.0) / (approved + rejected) : 0.0;

        return FreelancerDashboardDTO.builder()
                .totalEarnings(totalEarnings)
                .currentMonthEarnings(currentMonthEarnings)
                .totalCompletedContracts(totalCompletedContracts)
                .currentMonthCompletedContracts(currentMonthCompletedContracts)
                .totalActiveProjects(totalActiveProjects)
                .activeProjectsThisWeek(activeProjectsThisWeek)
                .successRate(successRate)
                .build();
    }


    public FreelancerEarningsDTO getEarningsOverview() {
        String freelancerId = com.giguniverse.backend.Auth.Session.AuthUtil.getUserId();
        if (freelancerId == null) throw new RuntimeException("User not authenticated");

        ZoneId utc = ZoneOffset.UTC;
        LocalDate now = LocalDate.now(utc);

        List<TransferEvent> allTransfers = transferEventRepo.findByFreelancerIdAndReversedFalse(freelancerId);
        double totalEarnings = allTransfers.stream()
                .mapToDouble(t -> t.getAmount() / 100.0)
                .sum();

        Instant currentMonthStart = now.withDayOfMonth(1).atStartOfDay(utc).toInstant();
        Instant nextMonthStart = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(utc).toInstant();

        List<TransferEvent> monthTransfers = transferEventRepo
                .findByFreelancerIdAndReversedFalseAndCreatedAtBetween(freelancerId, currentMonthStart, nextMonthStart);

        double currentMonthEarnings = monthTransfers.stream()
                .mapToDouble(t -> t.getAmount() / 100.0)
                .sum();

        final int N = 6;
        List<MonthlyEarningsDTO> last6Months = new ArrayList<>(N);
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

        for (int i = N - 1; i >= 0; i--) {
                LocalDate targetMonth = now.minusMonths(i);

                Instant start = targetMonth.withDayOfMonth(1).atStartOfDay(utc).toInstant();
                Instant end = targetMonth.plusMonths(1).withDayOfMonth(1).atStartOfDay(utc).toInstant();

                List<TransferEvent> transfers = transferEventRepo
                        .findByFreelancerIdAndReversedFalseAndCreatedAtBetween(freelancerId, start, end);

                double monthEarnings = transfers.stream()
                        .mapToDouble(t -> t.getAmount() / 100.0)
                        .sum();

                String monthLabel = targetMonth.format(monthFormatter);
                last6Months.add(new MonthlyEarningsDTO(monthLabel, monthEarnings));
        }

        double avgMonthly = last6Months.stream()
                .mapToDouble(MonthlyEarningsDTO::getAmount)
                .average()
                .orElse(0.0);

        return FreelancerEarningsDTO.builder()
                .totalEarnings(totalEarnings)
                .currentMonthEarnings(currentMonthEarnings)
                .avgMonthly(avgMonthly)
                .last6Months(last6Months) 
                .build();
        }


     public List<SkillStatsDTO> getTopSkills() {
        String freelancerId = AuthUtil.getUserId();

        // 1. Get completed contracts for freelancer
        List<Contract> contracts = contractRepo.findByFreelancerIdAndContractStatus(freelancerId, "completed");

        // Maps skill -> list of ratings & count
        Map<String, List<Integer>> skillRatings = new HashMap<>();
        Map<String, Integer> skillProjects = new HashMap<>();

        for (Contract c : contracts) {
            JobPost job = jobPostRepo.findById(Integer.valueOf(c.getJobId()))
                    .orElse(null);
            if (job == null || job.getSkillTags() == null) continue;

            String[] skills = job.getSkillTags().split(",");
            List<FreelancerFeedback> feedbacks = freelancerFeedbackRepo.findByContractId(c.getContractId());

            for (String skill : skills) {
                skill = skill.trim();

                skillProjects.put(skill, skillProjects.getOrDefault(skill, 0) + 1);

                for (FreelancerFeedback fb : feedbacks) {
                    skillRatings.computeIfAbsent(skill, k -> new ArrayList<>()).add(fb.getRating());
                }
            }
        }

        List<SkillStatsDTO> result = skillProjects.entrySet().stream()
                .map(e -> {
                    String skill = e.getKey();
                    int projects = e.getValue();
                    List<Integer> ratings = skillRatings.getOrDefault(skill, new ArrayList<>());

                    double avg = ratings.isEmpty() ? 0.0 :
                            ratings.stream().mapToInt(Integer::intValue).average().orElse(0.0);

                    return SkillStatsDTO.builder()
                            .skill(skill)
                            .projectsCompleted(projects)
                            .averageRating(Math.round(avg * 10.0) / 10.0) 
                            .build();
                })

                .sorted(
                Comparator.comparingDouble(SkillStatsDTO::getAverageRating).reversed()
                        .thenComparing(Comparator.comparingInt(SkillStatsDTO::getProjectsCompleted).reversed())
                )
                .limit(5)
                .collect(Collectors.toList());

        return result;
    }

    public Map<String, Long> getContractStatusCount(String freelancerId) {
        List<Object[]> results = contractRepo.countContractsByStatus(freelancerId);

        Map<String, Long> statusCount = new HashMap<>();
        for (Object[] row : results) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            statusCount.put(status, count);
        }
        return statusCount;
    }

   private String formatTimeAgo(Date date) {
        long diffMillis = System.currentTimeMillis() - date.getTime();
        long minutes = diffMillis / (1000 * 60);
        if (minutes < 60) return minutes + " minutes ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hours ago";
        long days = hours / 24;
        return days + " days ago";
    }

  public List<RecentEarningDTO> getRecentEarnings() {
        String freelancerId = AuthUtil.getUserId(); 

        List<TransferEvent> events = transferEventRepo.findByFreelancerIdOrderByCreatedAtDesc(freelancerId);

        return events.stream()
                .map(event -> {
                    Task task = taskRepo.findById(Integer.valueOf(event.getTaskId())).orElse(null);

                    String jobTitle = "Unknown Project";
                    if (task != null) {
                        jobTitle = jobPostRepo.findById(Integer.valueOf(task.getJobId()))
                                .map(JobPost::getJobTitle)
                                .orElse("Unknown Project");
                    }

                    String action = "Payment Received"; 
                    String amount = "$" + String.format("%.2f", event.getAmount() / 100.0);
                    String time = formatTimeAgo(Date.from(event.getCreatedAt()));

                    return RecentEarningDTO.builder()
                            .action(action)
                            .project(jobTitle + " - " + (task != null ? task.getTaskName() : "Unknown Task"))
                            .amount(amount)
                            .time(time)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<TaskDeadlineDTO> getUpcomingDeadlines() {
        String freelancerId = AuthUtil.getUserId();

        List<Task> tasks = taskRepo.findTop4ByFreelancerIdAndTaskStatusOrderByTaskDueDateAsc(
                freelancerId, "pending"
        );

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd");

        return tasks.stream()
                .map(task -> {
                        String clientName = "Unknown Client";

                        // Example: if Task has employerId
                        if (task.getEmployerId() != null) {
                        clientName = employerProfileRepo.findByEmployer_EmployerUserId(task.getEmployerId())
                                .map(EmployerProfile::getFullName) 
                                .orElse("Unknown Client");
                        }

                        return TaskDeadlineDTO.builder()
                                .project(task.getTaskName())
                                .client(clientName)
                                .deadline(task.getTaskDueDate() != null ? sdf.format(task.getTaskDueDate()) : "N/A")
                                .status("pending")
                                .build();
                })
                .collect(Collectors.toList());
        }

        public Optional<BannedInfoDTO> getBannedInfo() {
                String userId = AuthUtil.getUserId();

                return freelancerRepository.findByFreelancerUserIdAndAccountBannedStatusTrue(userId)
                .map(f -> {
                        long daysRemaining = 0;
                        if (f.getUnbanDate() != null) {
                        daysRemaining = Duration.between(LocalDateTime.now(), f.getUnbanDate()).toDays();
                        }
                        return new BannedInfoDTO(f.getBannedReason(), f.getUnbanDate(), daysRemaining);
                });
        }

}
