package com.giguniverse.backend.JobPost.HiredFreelancer.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.JobPost.HiredFreelancer.Service.HiredFreelancerService;

@RestController
@RequestMapping("/api/hired-freelancers")
public class HiredFreelancerController {

    @Autowired
    HiredFreelancerService hiredFreelancerService;

    @GetMapping("/employer-view")
    public ResponseEntity<List<Map<String, Object>>> getContractsForEmployer() {
        List<Map<String, Object>> contracts = hiredFreelancerService.getContractsForEmployer();
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/freelancer-view")
    public ResponseEntity<List<Map<String, Object>>> getContractsForFreelancer() {
        List<Map<String, Object>> contracts = hiredFreelancerService.getContractsForFreelancer();
        return ResponseEntity.ok(contracts);
    }
}
