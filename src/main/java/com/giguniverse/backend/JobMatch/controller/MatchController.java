package com.giguniverse.backend.JobMatch.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
}
