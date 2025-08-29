package com.giguniverse.backend.JobPost.CreateJobPost.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.JobPost.ApplyJob.Model.FavouriteJob;
import com.giguniverse.backend.JobPost.ApplyJob.Repository.FavouriteJobRepository;
import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPost;
import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPostRequestDTO;
import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPostUpdateRequest;
import com.giguniverse.backend.JobPost.CreateJobPost.repository.JobPostRepository;
import com.giguniverse.backend.Profile.Model.EmployerProfile;
import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;

@Service
public class JobPostService {

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    EmployerProfileRepository employerProfileRepository;

    @Autowired
    FavouriteJobRepository favouriteJobRepository;

    public List<JobPost> getJobPostsForEmployer(String employerId) {
        List<JobPost> jobs = jobPostRepository.findByEmployerIdOrderByCreatedAtDesc(employerId);
        return jobs;
    }

    public JobPost getJobPostsBasedOnId(Integer jobPostId) {
        JobPost jobPost = jobPostRepository.findByJobPostId(jobPostId)
                .stream()
                .findFirst()
                .orElse(null);

        if (jobPost != null && jobPost.getEmployerId() != null) {
            Optional<EmployerProfile> employerProfileOpt =
                    employerProfileRepository.findByEmployer_EmployerUserId(jobPost.getEmployerId());

            employerProfileOpt.ifPresent(profile -> {
                jobPost.setEmployerName(profile.getFullName()); 
            });
        }

        return jobPost;
    }

    public JobPost createJobPost(JobPostRequestDTO dto, String employerId) {
        // Fetch employer profile
        EmployerProfile employer = employerProfileRepository.findByEmployer_EmployerUserId(employerId)
                .orElseThrow(() -> new RuntimeException("Employer not found"));

        boolean isPremium = Boolean.TRUE.equals(employer.getPremiumStatus());

        int postingLimit = isPremium ? 5 : 2;

        Calendar startOfMonth = Calendar.getInstance();
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        startOfMonth.set(Calendar.MINUTE, 0);
        startOfMonth.set(Calendar.SECOND, 0);
        startOfMonth.set(Calendar.MILLISECOND, 0);

        Calendar endOfMonth = Calendar.getInstance();
        endOfMonth.set(Calendar.DAY_OF_MONTH, endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
        endOfMonth.set(Calendar.HOUR_OF_DAY, 23);
        endOfMonth.set(Calendar.MINUTE, 59);
        endOfMonth.set(Calendar.SECOND, 59);
        endOfMonth.set(Calendar.MILLISECOND, 999);

        long monthlyCount = jobPostRepository.countByEmployerIdAndCreatedAtBetween(
                employerId,
                startOfMonth.getTime(),
                endOfMonth.getTime()
        );


        if (monthlyCount >= postingLimit) {
            Map<String, Object> body = new HashMap<>();
            body.put("errorCode", "MONTHLY_LIMIT");
            body.put("message", "You have reached your monthly job posting limit (" + postingLimit + ")");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, body.toString());
        }

        JobPost jobPost = new JobPost();
        jobPost.setJobTitle(dto.getJobTitle());
        jobPost.setJobDescription(dto.getJobDescription());
        jobPost.setJobScope(dto.getJobScope());
        jobPost.setIsPremiumJob(isPremium);
        jobPost.setSkillTags(String.join(",", dto.getSkillTags()));
        jobPost.setJobField(dto.getJobField());
        jobPost.setJobCategory(String.join(",", dto.getJobCategory()));
        jobPost.setYearsOfJobExperience(dto.getYearsOfExperienceFrom() + "-" + dto.getYearsOfExperienceTo());
        jobPost.setJobExperience(String.join(",", dto.getJobExperience()));
        jobPost.setLanguageProficiency(String.join(",", dto.getLanguageProficiency()));

        if (dto.getHoursContributionPerWeek() != null && !dto.getHoursContributionPerWeek().isEmpty()) {
            jobPost.setHoursContributionPerWeek(Integer.parseInt(dto.getHoursContributionPerWeek()));
        }

        jobPost.setHighestEducationLevel(String.join(",", dto.getHighestEducationLevel()));
        jobPost.setJobStatus(dto.getJobStatus());

        if (dto.getPayRateFrom() != null && dto.getPayRateTo() != null) {
            jobPost.setPreferredPayrate(dto.getPayRateFrom() + "-" + dto.getPayRateTo());
        }

        jobPost.setDuration(dto.getDurationValue() + " " + dto.getDurationUnit());

        if ("Location-Specific Hiring".equalsIgnoreCase(dto.getJobLocationHiring())) {
            jobPost.setJobLocationHiringRequired(true);
            jobPost.setJobLocation(String.join(",", dto.getJobLocation()));
        } else {
            jobPost.setJobLocationHiringRequired(false);
            jobPost.setJobLocation("global");
        }

        if (isPremium){
            jobPost.setMaxApplicationNumber(50);
        } else {
            jobPost.setMaxApplicationNumber(30);
        }
        

        jobPost.setCompanyName(dto.getCompanyName());
        jobPost.setEmployerId(employerId);

        Date now = new Date();
        jobPost.setCreatedAt(now);
        jobPost.setUpdatedAt(now);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_MONTH, 30);
        jobPost.setJobExpirationDate(calendar.getTime());

        return jobPostRepository.save(jobPost);
    }

    public JobPost updateJobPost(int id, JobPostUpdateRequest req) {
        Optional<JobPost> optionalJobPost = jobPostRepository.findById(id);
        if (optionalJobPost.isEmpty()) {
            throw new RuntimeException("Job post not found");
        }

        JobPost job = optionalJobPost.get();

        job.setJobTitle(req.getJobTitle());
        job.setJobDescription(req.getJobDescription());
        job.setJobScope(req.getJobScope());
        job.setIsPremiumJob(req.isPremiumJob());
        job.setSkillTags(String.join(",", req.getSkillTags()));
        job.setJobField(req.getJobField());
        job.setJobCategory(String.join(",", req.getJobCategory()));
        job.setYearsOfJobExperience(req.getYearsOfExperienceFrom() + "-" + req.getYearsOfExperienceTo());
        job.setJobExperience(String.join(",", req.getJobExperience()));
        job.setLanguageProficiency(String.join(",", req.getLanguageProficiency()));

        if (req.getHoursContributionPerWeek() != null && !req.getHoursContributionPerWeek().isEmpty()) {
            job.setHoursContributionPerWeek(Integer.parseInt(req.getHoursContributionPerWeek()));
        }

        job.setHighestEducationLevel(String.join(",", req.getHighestEducationLevel()));
        job.setJobStatus(req.getJobStatus());

        if (req.getPayRateFrom() != null && req.getPayRateTo() != null) {
            job.setPreferredPayrate(req.getPayRateFrom() + "-" + req.getPayRateTo());
        }

        job.setDuration(req.getDurationValue() + " " + req.getDurationUnit());

        if ("Location-Specific Hiring".equalsIgnoreCase(req.getJobLocationHiring())) {
            job.setJobLocationHiringRequired(true);
            job.setJobLocation(String.join(",", req.getJobLocation()));
        } else {
            job.setJobLocationHiringRequired(false);
            job.setJobLocation("global");
        }

        job.setCompanyName(req.getCompanyName());

        Date now = new Date();

        job.setUpdatedAt(now);

        return jobPostRepository.save(job);
    }

    public int getJobPostsThisMonth(String employerId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59);

        Date startDate = Date.from(startOfMonth.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endOfMonth.atZone(ZoneId.systemDefault()).toInstant());

        return jobPostRepository.countByEmployerIdAndCreatedAtBetween(employerId, startDate, endDate);
    }

    public List<JobPost> getAllActiveJobs() {
        String currentUserId = AuthUtil.getUserId();
        List<JobPost> jobs = jobPostRepository.findByJobStatusNot(
            "Inactive",
            Sort.by(Sort.Direction.DESC, "createdAt") 
        );
        List<FavouriteJob> favJobs = favouriteJobRepository.findByFreelancerId(currentUserId);
        Set<String> favJobIds = favJobs.stream()
                                       .map(FavouriteJob::getJobId)
                                       .collect(Collectors.toSet());

        jobs.forEach(job -> job.setSaved(favJobIds.contains(String.valueOf(job.getJobPostId()))));

        return jobs;
    }
}