package com.giguniverse.backend.JobPost.ApplyJob.Service;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.Feedback.Model.FreelancerFeedback;
import com.giguniverse.backend.Feedback.Repository.FreelancerFeedbackRepository;
import com.giguniverse.backend.JobPost.ApplyJob.Model.FreelancerApplicationDTO;
import com.giguniverse.backend.JobPost.ApplyJob.Model.JobApplication;
import com.giguniverse.backend.JobPost.ApplyJob.Model.JobApplicationDTO;
import com.giguniverse.backend.JobPost.ApplyJob.Repository.JobApplicationRepository;
import com.giguniverse.backend.JobPost.ContractHandling.Repository.ContractRepository;
import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPost;
import com.giguniverse.backend.JobPost.CreateJobPost.repository.JobPostRepository;
import com.giguniverse.backend.Profile.Model.FreelancerProfile;
import com.giguniverse.backend.Profile.Repository.FreelancerProfileRepository;



@Service
public class JobApplicationService {

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private FreelancerProfileRepository freelancerProfileRepository;

    @Autowired
    private FreelancerFeedbackRepository freelancerFeedbackRepository;

    @Autowired
    private ContractRepository contractRepository;

    public JobApplication applyForJob(String jobId, String hourlyRate, String jobProposal, String freelancerId) {

        JobPost jobPost = jobPostRepository.findById(Integer.parseInt(jobId))
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (jobApplicationRepository.countByJobId(jobId) >= jobPost.getMaxApplicationNumber()) {
            if (!"Full".equalsIgnoreCase(jobPost.getJobStatus())) {
                jobPost.setJobStatus("Full");
                jobPostRepository.save(jobPost);
            }
            throw new RuntimeException("This job has reached the maximum number of applications.");
        }

        JobApplication application = new JobApplication();
        application.setJobId(jobId);
        application.setHourlyRate(hourlyRate);
        application.setJobProposal(jobProposal);
        application.setFreelancerId(freelancerId);
        application.setApplicationStatus("pending");
        application.setAppliedDate(new Date());

        JobApplication savedApp = jobApplicationRepository.save(application);

        int updatedApplications = jobApplicationRepository.countByJobId(jobId);
        if (updatedApplications >= jobPost.getMaxApplicationNumber()) {
            jobPost.setJobStatus("Full");
            jobPostRepository.save(jobPost);
        }

        return savedApp;
    }

    public List<JobApplicationDTO> getApplicationsForJob(String jobId) {
        List<JobApplication> applications = jobApplicationRepository.findByJobId(jobId);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

        // Define ordering priority for status
        Map<String, Integer> statusOrder = Map.of(
            "pending", 1,
            "contract", 2,
            "shortlisted", 3,
            "rejected", 4
        );

        return applications.stream()
            .sorted((a, b) -> {
                int orderA = statusOrder.getOrDefault(a.getApplicationStatus(), 99);
                int orderB = statusOrder.getOrDefault(b.getApplicationStatus(), 99);

                if (orderA != orderB) {
                    return Integer.compare(orderA, orderB);
                }
                return b.getAppliedDate().compareTo(a.getAppliedDate());
            })
            .map(app -> {
                Optional<FreelancerProfile> profile = freelancerProfileRepository
                        .findByFreelancer_FreelancerUserId(app.getFreelancerId());

                String avatar = null;
                if (profile.isPresent() && profile.get().getProfilePicture() != null) {
                    avatar = "data:" + profile.get().getProfilePictureMimeType() + ";base64," +
                            Base64.getEncoder().encodeToString(profile.get().getProfilePicture());
                }

                List<FreelancerFeedback> feedbackList = freelancerFeedbackRepository
                                .findByFreelancerId(app.getFreelancerId());

                double rating = feedbackList.isEmpty() ? 0 :
                    feedbackList.stream().mapToDouble(FreelancerFeedback::getRating).average().orElse(0);

                long completedJobs = contractRepository
                    .countByFreelancerIdAndContractStatus(app.getFreelancerId(), "completed");


                return new JobApplicationDTO(
                        app.getJobApplicationId(),
                        app.getFreelancerId(),
                        profile.map(FreelancerProfile::getFullName).orElse("Unknown"),
                        avatar,
                        app.getHourlyRate() != null ? Integer.valueOf(app.getHourlyRate()) : null,
                        app.getJobProposal(),
                        sdf.format(app.getAppliedDate()),
                        app.getApplicationStatus(),
                        rating,
                        completedJobs
                );
            })
            .collect(Collectors.toList());
    }


    public Optional<JobApplication> rejectApplication(int id, String rejectInfo) {
        Optional<JobApplication> applicationOpt = jobApplicationRepository.findById(id);

        if (applicationOpt.isEmpty()) {
            return Optional.empty();
        }

        JobApplication application = applicationOpt.get();
        application.setApplicationStatus("rejected");

        if (rejectInfo != null && !rejectInfo.isBlank()) {
            application.setRejectInfo(rejectInfo);
        }

        jobApplicationRepository.save(application);
        return Optional.of(application);
    }


    public Optional<JobApplication> shortlistApplication(int id) {
        Optional<JobApplication> applicationOpt = jobApplicationRepository.findById(id);

        if (applicationOpt.isEmpty()) {
            return Optional.empty();
        }

        JobApplication application = applicationOpt.get();
        application.setApplicationStatus("shortlisted");
        jobApplicationRepository.save(application);

        return Optional.of(application);
    }

    public Optional<JobApplication> unshortlistApplication(int id) {
        Optional<JobApplication> applicationOpt = jobApplicationRepository.findById(id);

        if (applicationOpt.isEmpty()) {
            return Optional.empty();
        }

        JobApplication application = applicationOpt.get();
        application.setApplicationStatus("pending");
        jobApplicationRepository.save(application);

        return Optional.of(application);
    }


    public List<FreelancerApplicationDTO> getApplicationsForFreelancer(String freelancerId) {
        List<JobApplication> apps = jobApplicationRepository.findByFreelancerId(freelancerId);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");

        List<String> statusOrder = Arrays.asList("contract", "pending", "shortlisted", "rejected");

        return apps.stream()
                .map(app -> {
                    JobPost job = jobPostRepository.findById(Integer.valueOf(app.getJobId()))
                            .orElse(null);

                    if (job == null) return null;

                    return new FreelancerApplicationDTO(
                            app.getJobApplicationId(),
                            job.getJobTitle(),
                            job.getCompanyName(),
                            sdf.format(app.getAppliedDate()),
                            app.getApplicationStatus(),
                            app.getRejectInfo() != null ? app.getRejectInfo() : "",
                            job.getYearsOfJobExperience(),
                            job.getJobExperience(),
                            job.getPreferredPayrate(),
                            app.getHourlyRate() != null ? "$" + app.getHourlyRate() : null,
                            job.getJobLocation() != null ? job.getJobLocation() : "global",
                            job.getJobCategory(),
                            app.getJobProposal(),
                            String.valueOf(job.getJobPostId()),
                            job.getEmployerId()
                    );
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    int statusComparison = Integer.compare(
                            statusOrder.indexOf(a.getStatus().toLowerCase()),
                            statusOrder.indexOf(b.getStatus().toLowerCase())
                    );

                    if (statusComparison != 0) {
                        return statusComparison;
                    }

                    try {
                        Date dateA = sdf.parse(a.getAppliedDate());
                        Date dateB = sdf.parse(b.getAppliedDate());
                        return dateB.compareTo(dateA); 
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
    }

}
