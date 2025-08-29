package com.giguniverse.backend.JobPost.CreateJobPost.controller;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPost;
import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPostRequestDTO;
import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPostUpdateRequest;
import com.giguniverse.backend.JobPost.CreateJobPost.service.JobPostService;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/job-posts")
public class JobPostController {

    private final JobPostService jobPostService;

    public JobPostController(JobPostService jobPostService) {
        this.jobPostService = jobPostService;
    }

    @PostMapping("/create")
    public ResponseEntity<JobPost> createJobPost(@RequestBody JobPostRequestDTO dto) {
        String employerId = AuthUtil.getUserId();
        JobPost savedJob = jobPostService.createJobPost(dto, employerId);
        return ResponseEntity.ok(savedJob);
    }


    @GetMapping("/fetch-jobs")
    public ResponseEntity<List<JobPost>> getEmployerJobs(HttpServletRequest request) {
        String employerId = AuthUtil.getUserId();
        List<JobPost> jobs = jobPostService.getJobPostsForEmployer(employerId);
        return ResponseEntity.ok(jobs);
    }


    @GetMapping("/fetch-job/{id}")
    public ResponseEntity<JobPost> getJobById(@PathVariable Integer id) { 
        JobPost job = jobPostService.getJobPostsBasedOnId(id);
        if (job != null) {
            return ResponseEntity.ok(job);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<JobPost> updateJobPost(
            @PathVariable int id,
            @RequestBody JobPostUpdateRequest request
    ) {
        JobPost updated = jobPostService.updateJobPost(id, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/employer/monthly-count")
    public Map<String, Object> getJobPostsThisMonth() {
        String employerId = AuthUtil.getUserId();
        int count = jobPostService.getJobPostsThisMonth(employerId);

        LocalDateTime nextReset = LocalDate.now()
                .withDayOfMonth(1)
                .plusMonths(1)
                .atStartOfDay();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String formattedReset = nextReset.format(formatter);

        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("resetDate", formattedReset);
        return response;
    }

    @GetMapping("/freelancer/job-search")
    public ResponseEntity<List<JobPost>> getAllActiveJobs() {
        List<JobPost> jobs = jobPostService.getAllActiveJobs();
        return ResponseEntity.ok(jobs);
    }
}
