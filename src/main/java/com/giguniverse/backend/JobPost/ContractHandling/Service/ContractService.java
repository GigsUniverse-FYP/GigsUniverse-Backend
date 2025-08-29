package com.giguniverse.backend.JobPost.ContractHandling.Service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.JobPost.ApplyJob.Repository.JobApplicationRepository;
import com.giguniverse.backend.JobPost.ContractHandling.Model.Contract;
import com.giguniverse.backend.JobPost.ContractHandling.Model.ContractDetailsDTO;
import com.giguniverse.backend.JobPost.ContractHandling.Repository.ContractRepository;
import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPost;
import com.giguniverse.backend.JobPost.CreateJobPost.repository.JobPostRepository;
import com.giguniverse.backend.Profile.Model.EmployerProfile;
import com.giguniverse.backend.Profile.Model.FreelancerProfile;
import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;
import com.giguniverse.backend.Profile.Repository.FreelancerProfileRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class ContractService {

    @Autowired
    private JobPostRepository jobPostRepository;
    @Autowired
    private EmployerProfileRepository employerProfileRepository;
    @Autowired
    private FreelancerProfileRepository freelancerProfileRepository;
    @Autowired
    private JobApplicationRepository jobApplicationRepository;
    @Autowired
    private ContractRepository contractRepository;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private FreelancerRepository freelancerRepository;

    public ContractDetailsDTO getContractDetails(Integer jobId, String employerId, String freelancerId) {
        // Fetch job
        JobPost job = jobPostRepository.findById(jobId).orElse(null);
        if (job == null) {
            throw new RuntimeException("Job not found");
        }

        // Fetch employer
        EmployerProfile employer = employerProfileRepository
                .findByEmployer_EmployerUserId(employerId)
                .orElse(null);

        // Fetch freelancer
        FreelancerProfile freelancer = freelancerProfileRepository
                .findByFreelancer_FreelancerUserId(freelancerId)
                .orElse(null);

        return new ContractDetailsDTO(
                job.getJobPostId(),
                job.getJobTitle(),
                employerId,
                employer != null ? employer.getFullName() : "Unknown Employer",
                freelancerId,
                freelancer != null ? freelancer.getFullName() : "Unknown Freelancer"
        );
    }

    private void sendContractEmail(String recipientEmail, Contract contract) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            JobPost jobPost = jobPostRepository.findById(Integer.parseInt(contract.getJobId()))
                .orElseThrow(() -> new RuntimeException("Job not found"));

            String jobName = jobPost.getJobTitle();

            String body = "<div style='font-family:Arial,sans-serif;font-size:14px;color:#333;'>"
                + "<p>Dear User,</p>"
                + "<p>A new <strong>Contract</strong> has been created for your application.</p>"
                + "<p><strong>Job ID:</strong> " + contract.getJobId() + "</p>"
                + "<p><strong>Job ID:</strong> " + jobName + "</p>"
                + "<p><strong>Agreed Pay Rate (per hour):</strong> $" + contract.getAgreedPayRatePerHour() + "</p>"
                + "<p><strong>Hours per Week:</strong> " + contract.getHourPerWeek() + "</p>"
                + "<p><strong>Contract Period:</strong> " 
                    + contract.getContractStartDate() + " to " + contract.getContractEndDate() + "</p>"
                + "<p><strong>Status:</strong> " + contract.getContractStatus() + "</p>"
                + "<br>"
                + "<p><strong>Please note:</strong> If you do not respond to this contract within <strong>3 days</strong>, "
                + "the contract will be automatically <span style='color:red;'>rejected</span>.</p>"
                + "<br>"
                + "<p>Please login to your account to review the contract.</p>"
                + "<br>"
                + "<p>Regards,<br>GigsUniverse Team</p>"
                + "</div>";

            helper.setFrom("admin@gigsuniverse.studio");
            helper.setTo(recipientEmail);
            helper.setSubject("New Contract Created - GigsUniverse");
            helper.setText(body, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }


    public Contract createContract(Contract contractRequest) {
        Contract contract = new Contract();
        contract.setAgreedPayRatePerHour(contractRequest.getAgreedPayRatePerHour());
        contract.setHourPerWeek(contractRequest.getHourPerWeek());
        contract.setContractStartDate(contractRequest.getContractStartDate());
        contract.setContractEndDate(contractRequest.getContractEndDate());
        contract.setJobId(contractRequest.getJobId());
        contract.setEmployerId(contractRequest.getEmployerId());
        contract.setFreelancerId(contractRequest.getFreelancerId());
        contract.setJobApplicationId(contractRequest.getJobApplicationId());

        // Defaults
        contract.setContractStatus("pending");
        contract.setContractCreationDate(new Date());
        contract.setFreelancerFeedback(false);
        contract.setApproveEarlyCancellation(null);
        contract.setCancellationReason(null);

        // update JobApplication status
        if (contractRequest.getJobApplicationId() != null) {
            int appId = Integer.parseInt(contractRequest.getJobApplicationId());
            jobApplicationRepository.findById(appId).ifPresent(jobApplication -> {
                jobApplication.setApplicationStatus("contract");
                jobApplicationRepository.save(jobApplication);

                String freelancerId = jobApplication.getFreelancerId();

                freelancerRepository.findById(freelancerId).ifPresent(freelancer -> {
                    String freelancerEmail = freelancer.getEmail();
                    sendContractEmail(freelancerEmail, contract);
                });
            });


        }

        return contractRepository.save(contract);
    }
}
