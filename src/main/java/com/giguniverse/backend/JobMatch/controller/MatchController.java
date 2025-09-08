package com.giguniverse.backend.JobMatch.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.giguniverse.backend.JobMatch.model.JobMatchResponse;
import com.giguniverse.backend.JobMatch.service.MatchService;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    @Autowired
    private MatchService matchService;

    @PostMapping("/freelancer/{freelancerId}/job/{jobPostId}")
    public Map<String, Object> matchFreelancerToJob(
            @PathVariable String freelancerId,
            @PathVariable Integer jobPostId) {

        return matchService.matchFreelancerToJob(freelancerId, jobPostId);
    }

    @GetMapping("/top-jobs")
    public List<JobMatchResponse> getTopJobMatchesForFreelancer() {
        return matchService.getTopMatchesForCurrentUser();
    }

    @GetMapping("/top-talents")
    public ResponseEntity<List<Map<String, Object>>> getTopTalents() {
        List<Map<String, Object>> topTalents = matchService.getTopTalentsForEmployer();
        return ResponseEntity.ok(topTalents);
    }

    @PostMapping("/send-invite")
    public ResponseEntity<?> sendInvite(@RequestBody Map<String, Object> request) {
        String freelancerUserId = (String) request.get("freelancerUserId");
        Integer jobPostId = (Integer) request.get("jobPostId");
        matchService.sendInvitationEmail(freelancerUserId, jobPostId);
        return ResponseEntity.ok(Map.of("message", "Invitation sent successfully"));
    }
}
