package com.giguniverse.backend.JobPost.ContractHandling.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.giguniverse.backend.JobPost.ContractHandling.Model.Contract;
import com.giguniverse.backend.JobPost.ContractHandling.Repository.ContractRepository;

import java.util.Date;
import java.util.List;

@Service
public class ContractScheduler {

    @Autowired
    private ContractRepository contractRepository;

    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void autoRejectExpiredContracts() {
        List<Contract> pendingContracts = contractRepository.findByContractStatus("pending");

        Date now = new Date();

        for (Contract contract : pendingContracts) {
            long diffMillis = now.getTime() - contract.getContractCreationDate().getTime();
            long diffDays = diffMillis / (1000 * 60 * 60 * 24);

            if (diffDays >= 3) {
                contract.setContractStatus("rejected");
                contract.setCancellationReason("Auto-rejected due to no response after 3 days");
                contractRepository.save(contract);
            }
        }
    }
}
