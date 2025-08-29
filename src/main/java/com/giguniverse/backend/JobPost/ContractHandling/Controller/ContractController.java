package com.giguniverse.backend.JobPost.ContractHandling.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
