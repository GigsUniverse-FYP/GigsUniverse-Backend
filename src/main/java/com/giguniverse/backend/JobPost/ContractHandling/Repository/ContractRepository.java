package com.giguniverse.backend.JobPost.ContractHandling.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.JobPost.ContractHandling.Model.Contract;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Integer> {
    List<Contract> findByContractStatus(String status);
}
