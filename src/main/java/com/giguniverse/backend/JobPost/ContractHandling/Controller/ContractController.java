package com.giguniverse.backend.JobPost.ContractHandling.Controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.JobPost.ContractHandling.Model.Contract;
import com.giguniverse.backend.JobPost.ContractHandling.Model.ContractDetailsDTO;
import com.giguniverse.backend.JobPost.ContractHandling.Service.ContractService;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @GetMapping("/details")
    public ResponseEntity<ContractDetailsDTO> getContractDetails(
            @RequestParam Integer jobId,
            @RequestParam String employerId,
            @RequestParam String freelancerId) {

        try {
            ContractDetailsDTO details = contractService.getContractDetails(jobId, employerId, freelancerId);
            return ResponseEntity.ok(details);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Contract> createContract(@RequestBody Contract contractRequest) {
        Contract savedContract = contractService.createContract(contractRequest);
        return ResponseEntity.ok(savedContract);
    }

    @GetMapping("/contract-details")
    public Map<String, Object> getContractDetails(@RequestParam String jobApplicationId) throws Exception {
        return contractService.getContractDetails(jobApplicationId);
    }

    @PostMapping("/reject")
    public ResponseEntity<String> rejectContract(
            @RequestParam String jobApplicationId,
            @RequestParam String freelancerId) {
        try {
            contractService.rejectContract(jobApplicationId, freelancerId);
            return ResponseEntity.ok("Contract rejected successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<String> submitContract(
            @RequestParam String jobApplicationId,
            @RequestParam String freelancerId) {
        try {
            contractService.submitContract(jobApplicationId, freelancerId);
            return ResponseEntity.ok("Contract submitted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getContractStatus(@RequestParam String jobApplicationId) {
        String status = contractService.getContractStatus(jobApplicationId);
        return ResponseEntity.ok(Collections.singletonMap("contractStatus", status));
    }

    @PostMapping("/statuses")
    public ResponseEntity<Map<String, String>> getContractStatuses(@RequestBody List<String> jobApplicationIds) {
        Map<String, String> statuses = contractService.getContractStatuses(jobApplicationIds);
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/count-active")
    public ResponseEntity<Map<String, Integer>> getActiveContractsCount() {
        String employerId = AuthUtil.getUserId();
        int count = contractService.countEmployerActiveContracts(employerId);
        return ResponseEntity.ok(Collections.singletonMap("count", count));
    }

    @GetMapping("/freelancer/count-active")
    public ResponseEntity<Map<String, Integer>> getFreelancerActiveContracts(@RequestParam String freelancerId) {
        int count = contractService.countActiveContractsByFreelancer(freelancerId);
        return ResponseEntity.ok(Collections.singletonMap("count", count));
    }
}
