package com.giguniverse.backend.JobPost.HiredFreelancer.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.Auth.Session.AuthUtil;
import com.giguniverse.backend.JobPost.ContractHandling.Model.Contract;
import com.giguniverse.backend.JobPost.ContractHandling.Repository.ContractRepository;
import com.giguniverse.backend.JobPost.CreateJobPost.repository.JobPostRepository;
import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;
import com.giguniverse.backend.Profile.Repository.FreelancerProfileRepository;

@Service
public class HiredFreelancerService {
   
    @Autowired
    EmployerProfileRepository employerProfileRepository;
    @Autowired
    ContractRepository contractRepository;
    @Autowired
    FreelancerProfileRepository freelancerProfileRepository;
    @Autowired
    JobPostRepository jobPostRepository;

   public List<Map<String, Object>> getContractsForEmployer() {
    String currentEmployerId = AuthUtil.getUserId();

    // Fetch employer name once
    String employerName = employerProfileRepository.findByEmployer_EmployerUserId(currentEmployerId)
            .map(employer -> employer.getFullName())
            .orElse("Unknown Employer");

    // Fetch contracts where current user is the employer
    List<Contract> contracts = contractRepository.findByEmployerId(currentEmployerId);

    // Define allowed contract statuses
    List<String> allowedStatuses = List.of("active", "upcoming", "completed", "cancelled");

    // Sort by status priority
    List<String> statusOrder = List.of("active", "upcoming", "completed", "cancelled");

    return contracts.stream()
            .filter(c -> allowedStatuses.contains(c.getContractStatus())) // exclude pending/rejected
            .sorted(Comparator.comparingInt(c -> statusOrder.indexOf(c.getContractStatus())))
            .map(contract -> {
                Map<String, Object> map = new HashMap<>();
                map.put("contractId", contract.getContractId());
                map.put("jobId", contract.getJobId());
                map.put("startDate", contract.getContractStartDate());
                map.put("endDate", contract.getContractEndDate());
                map.put("status", contract.getContractStatus());
                map.put("employerName", employerName);
                map.put("hourlyRate", contract.getAgreedPayRatePerHour());
                map.put("totalHours", contract.getHourPerWeek());
                map.put("employerId", contract.getEmployerId());
                
                jobPostRepository.findByJobPostId(Integer.parseInt(contract.getJobId())).ifPresent(jobPost -> {
                    map.put("companyName", jobPost.getCompanyName());
                });

                // Fetch freelancer details
                freelancerProfileRepository.findByFreelancer_FreelancerUserId(contract.getFreelancerId())
                        .ifPresent(freelancer -> {
                            map.put("freelancerName", freelancer.getFullName());
                            map.put("freelancerId", freelancer.getFreelancer().getFreelancerUserId());
                            if (freelancer.getProfilePicture() != null) {
                                String base64Image = java.util.Base64.getEncoder().encodeToString(freelancer.getProfilePicture());
                                map.put("avatar", "data:" + freelancer.getProfilePictureMimeType() + ";base64," + base64Image);
                            } else {
                                map.put("avatar", null);
                            }
                        });

                // Fetch job details
                jobPostRepository.findById(Integer.parseInt(contract.getJobId()))
                        .ifPresent(job -> map.put("jobName", job.getJobTitle()));

                return map;
            })
            .toList();
        }

    public List<Map<String, Object>> getContractsForFreelancer() {
        String currentFreelancerId = AuthUtil.getUserId();

        // Fetch contracts where current user is the freelancer
        List<Contract> contracts = contractRepository.findByFreelancerId(currentFreelancerId);

        // Allowed statuses
        List<String> allowedStatuses = List.of("active", "upcoming", "completed", "cancelled");
        List<String> statusOrder = List.of("active", "upcoming", "completed", "cancelled");

        return contracts.stream()
                .filter(c -> allowedStatuses.contains(c.getContractStatus()))
                .sorted(Comparator.comparingInt(c -> statusOrder.indexOf(c.getContractStatus())))
                .map(contract -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("contractId", contract.getContractId());
                    map.put("jobId", contract.getJobId());
                    map.put("startDate", contract.getContractStartDate());
                    map.put("endDate", contract.getContractEndDate());
                    map.put("status", contract.getContractStatus());
                    map.put("hourlyRate", contract.getAgreedPayRatePerHour());
                    map.put("totalHours", contract.getHourPerWeek());
                    map.put("freelancerId", currentFreelancerId);
                    map.put("freelancerFeedback", contract.getFreelancerFeedback());

                    // Fetch job (for company + job name)
                    jobPostRepository.findByJobPostId(Integer.parseInt(contract.getJobId()))
                            .ifPresent(jobPost -> {
                                map.put("companyName", jobPost.getCompanyName());
                                map.put("jobName", jobPost.getJobTitle());
                            });

                    employerProfileRepository.findByEmployer_EmployerUserId(contract.getEmployerId())
                            .ifPresent(employer -> {
                                map.put("employerName", employer.getFullName());
                                map.put("employerId", employer.getEmployer().getEmployerUserId());
                            });

                    freelancerProfileRepository.findByFreelancer_FreelancerUserId(contract.getFreelancerId())
                            .ifPresent(freelancer -> {
                                map.put("name", freelancer.getFullName());
                            });

                    return map;
                })
                .toList();
        }


}
