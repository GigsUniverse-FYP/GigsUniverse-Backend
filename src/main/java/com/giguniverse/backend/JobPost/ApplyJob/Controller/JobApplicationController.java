package com.giguniverse.backend.JobPost.ApplyJob.Controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.JobPost.ApplyJob.Model.FreelancerApplicationDTO;
import com.giguniverse.backend.JobPost.ApplyJob.Model.JobApplication;
import com.giguniverse.backend.JobPost.ApplyJob.Model.JobApplicationDTO;
import com.giguniverse.backend.JobPost.ApplyJob.Repository.JobApplicationRepository;
import com.giguniverse.backend.JobPost.ApplyJob.Service.JobApplicationService;

@RestController
@RequestMapping("/api/job-applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;
    private final JobApplicationRepository jobApplicationRepository;

    public JobApplicationController(JobApplicationService jobApplicationService,
            JobApplicationRepository jobApplicationRepository) {
        this.jobApplicationService = jobApplicationService;
        this.jobApplicationRepository = jobApplicationRepository;
    }

    @PostMapping("/apply")
    public ResponseEntity<?> applyForJob(@RequestBody Map<String, String> payload) {
        try {
            String jobId = payload.get("jobId");
            String hourlyRate = payload.get("ratePerHour");
            String proposal = payload.get("proposal");

            String freelancerId = AuthUtil.getUserId();

            JobApplication application = jobApplicationService.applyForJob(jobId, hourlyRate, proposal, freelancerId);

            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkIfApplied(@RequestParam String jobId) {
        boolean applied = jobApplicationRepository.existsByJobIdAndFreelancerId(jobId, AuthUtil.getUserId());
        return ResponseEntity.ok(Map.of("applied", applied));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<JobApplicationDTO>> getApplicationsForJob(@PathVariable String jobId) {
        return ResponseEntity.ok(jobApplicationService.getApplicationsForJob(jobId));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<JobApplication> rejectApplication(
            @PathVariable int id,
            @RequestBody(required = false) String rejectInfo) {

        Optional<JobApplication> application = jobApplicationService.rejectApplication(id, rejectInfo);

        return application
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/shortlist")
    public ResponseEntity<JobApplication> shortlistApplication(@PathVariable int id) {
        return jobApplicationService.shortlistApplication(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/unshortlist")
    public ResponseEntity<JobApplication> unshortlistApplication(@PathVariable int id) {
        return jobApplicationService.unshortlistApplication(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/freelancer/applications")
    public List<FreelancerApplicationDTO> getFreelancerApplications() {
        String freelancerId = AuthUtil.getUserId(); 
        return jobApplicationService.getApplicationsForFreelancer(freelancerId);
    }
}