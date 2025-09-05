package com.giguniverse.backend.JobPost.ContractHandling.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.JobPost.ContractHandling.Model.Contract;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Integer> {
    List<Contract> findByContractStatus(String status);
    Optional<Contract> findByJobApplicationId(String jobApplicationId);
    Optional<Contract> findByJobApplicationIdAndFreelancerId(String jobApplicationId, String freelancerId);
    int countByEmployerIdAndContractStatusIn(String employerId, List<String> statuses);

    List<Contract> findByJobId(String jobId);

    @Query("SELECT COUNT(c) FROM Contract c WHERE c.freelancerId = :freelancerId " +
       "AND (c.contractStatus IN ('upcoming', 'active') " +
       "OR (c.contractStatus = 'completed' AND c.freelancerFeedback = false))")
    int countFreelancerEligibleContracts(@Param("freelancerId") String freelancerId);

    List<Contract> findByEmployerId(String currentEmployerId);
    List<Contract> findByFreelancerId(String currentFreelancerId);

    long countByFreelancerIdAndContractStatusAndContractEndDateIsNotNull(
            String freelancerId, String contractStatus
    );

    long countByFreelancerIdAndContractStatus(
        String freelancerId, String contractStatus
    );

    long countByFreelancerIdAndContractStatusAndContractEndDateBetween(
            String freelancerId, String contractStatus, Date start, Date end
    );

    List<Contract> findByFreelancerIdAndContractStatus(String freelancerId, String contractStatus);

   @Query("SELECT c.contractStatus, COUNT(c) " +
           "FROM Contract c " +
           "WHERE c.freelancerId = :freelancerId " +
           "GROUP BY c.contractStatus")
    List<Object[]> countContractsByStatus(@Param("freelancerId") String freelancerId);

       @Query("SELECT c.contractStatus, COUNT(c) " +
           "FROM Contract c " +
           "WHERE c.employerId = :employerId " +
           "GROUP BY c.contractStatus")
    List<Object[]> countEmployerContractsByStatus(@Param("employerId") String employerId);

    List<Contract> findByContractStatusAndContractEndDateBetween(
        String contractStatus, Date start, Date end
    );
}
