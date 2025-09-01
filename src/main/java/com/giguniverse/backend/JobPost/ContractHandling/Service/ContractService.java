package com.giguniverse.backend.JobPost.ContractHandling.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.giguniverse.backend.Auth.Repository.FreelancerRepository;
import com.giguniverse.backend.Feedback.Model.EmployerFeedback;
import com.giguniverse.backend.Feedback.Model.EmployerFeedbackDTO;
import com.giguniverse.backend.Feedback.Model.FreelancerFeedback;
import com.giguniverse.backend.Feedback.Model.FreelancerFeedbackDTO;
import com.giguniverse.backend.Feedback.Service.EmployerFeedbackService;
import com.giguniverse.backend.Feedback.Service.FreelancerFeedbackService;
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
    @Autowired
    private FreelancerFeedbackService feedbackService;
    @Autowired
    private EmployerFeedbackService employerFeedbackService;

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

    private void sendRejectEmailToEmployer(String employerEmail, Contract contract) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            JobPost jobPost = jobPostRepository.findById(Integer.parseInt(contract.getJobId()))
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            FreelancerProfile freelancer = freelancerProfileRepository.findByFreelancer_FreelancerUserId(contract.getFreelancerId())
                    .orElseThrow(() -> new RuntimeException("Freelancer not found"));

            String freelancerName = freelancer.getFullName();

            String jobName = jobPost.getJobTitle();

            String body = "<div style='font-family:Arial,sans-serif;font-size:14px;color:#333;'>"
                    + "<p>Dear Employer,</p>"
                    + "<p>The freelancer <strong>" + freelancerName + "</strong> has <span style='color:red;'>rejected</span> the contract for the following job:</p>"
                    + "<p><strong>Job:</strong> " + jobName + " (ID: " + contract.getJobId() + ")</p>"
                    + "<p><strong>Contract Period:</strong> " 
                        + contract.getContractStartDate() + " to " + contract.getContractEndDate() + "</p>"
                    + "<p><strong>Agreed Pay Rate:</strong> $" + contract.getAgreedPayRatePerHour() + "</p>"
                    + "<p><strong>Hours per Week:</strong> " + contract.getHourPerWeek() + "</p>"
                    + "<p><strong>Status:</strong> " + contract.getContractStatus() + "</p>"
                    + "<br>"
                    + "<p>Please login to your account to take further action.</p>"
                    + "<br>"
                    + "<p>Regards,<br>GigsUniverse Team</p>"
                    + "</div>";

            helper.setFrom("admin@gigsuniverse.studio");
            helper.setTo(employerEmail);
            helper.setSubject("Contract Rejected by Freelancer - GigsUniverse");
            helper.setText(body, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private void sendAcceptEmailToEmployer(String employerEmail, Contract contract) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            JobPost jobPost = jobPostRepository.findById(Integer.parseInt(contract.getJobId()))
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            String jobName = jobPost.getJobTitle();

            
            FreelancerProfile freelancer = freelancerProfileRepository.findByFreelancer_FreelancerUserId(contract.getFreelancerId())
                    .orElseThrow(() -> new RuntimeException("Freelancer not found"));

            String freelancerName = freelancer.getFullName();

            String body = "<div style='font-family:Arial,sans-serif;font-size:14px;color:#333;'>"
                    + "<p>Dear Employer,</p>"
                    + "<p>The freelancer <strong>" + freelancerName + "</strong> has <span style='color:green;'>accepted</span> the contract for the following job:</p>"
                    + "<p><strong>Job:</strong> " + jobName + " (ID: " + contract.getJobId() + ")</p>"
                    + "<p><strong>Contract Period:</strong> " 
                        + contract.getContractStartDate() + " to " + contract.getContractEndDate() + "</p>"
                    + "<p><strong>Agreed Pay Rate:</strong> $" + contract.getAgreedPayRatePerHour() + "</p>"
                    + "<p><strong>Hours per Week:</strong> " + contract.getHourPerWeek() + "</p>"
                    + "<p><strong>Status:</strong> " + contract.getContractStatus() + "</p>"
                    + "<br>"
                    + "<p>Please login to your account to manage the contract.</p>"
                    + "<br>"
                    + "<p>Regards,<br>GigsUniverse Team</p>"
                    + "</div>";

            helper.setFrom("admin@gigsuniverse.studio");
            helper.setTo(employerEmail);
            helper.setSubject("Contract Accepted by Freelancer - GigsUniverse");
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

    // freelancer fetching contract details
    public Map<String, Object> getContractDetails(String jobApplicationId) throws Exception {
        Contract contract = contractRepository.findByJobApplicationId(jobApplicationId)
                .orElseThrow(() -> new Exception("Contract not found"));

        String employerName = employerProfileRepository.findByEmployer_EmployerUserId(contract.getEmployerId())
                .map(e -> e.getFullName())
                .orElse("Unknown Employer");

        String freelancerName = freelancerProfileRepository.findByFreelancer_FreelancerUserId(contract.getFreelancerId())
                .map(f -> f.getFullName())
                .orElse("Unknown Freelancer");

        String jobTitle = jobPostRepository.findById(Integer.parseInt(contract.getJobId()))
                .map(job -> job.getJobTitle())
                .orElse("Unknown Job");

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", contract.getJobId());
        response.put("jobTitle", jobTitle);
        response.put("jobApplicationId", contract.getJobApplicationId());
        response.put("employerId", contract.getEmployerId());
        response.put("employerName", employerName);
        response.put("freelancerId", contract.getFreelancerId());
        response.put("freelancerName", freelancerName);
        response.put("payRate", contract.getAgreedPayRatePerHour());
        response.put("hoursPerWeek", contract.getHourPerWeek());
        response.put("startDate", contract.getContractStartDate());
        response.put("endDate", contract.getContractEndDate());

        return response;
    }

    public void rejectContract(String jobApplicationId, String freelancerId) throws Exception {
        Contract contract = contractRepository
                .findByJobApplicationIdAndFreelancerId(jobApplicationId, freelancerId)
                .orElseThrow(() -> new Exception("Contract not found"));

        contract.setContractStatus("rejected");

        EmployerProfile employer = employerProfileRepository.findByEmployer_EmployerUserId(contract.getEmployerId())
                .orElseThrow(() -> new RuntimeException("Employer not found"));

        String employerEmail = employer.getEmail();

        sendRejectEmailToEmployer(employerEmail, contract);

        contractRepository.save(contract);
    }

    public void submitContract(String jobApplicationId, String freelancerId) throws Exception {
        Contract contract = contractRepository
                .findByJobApplicationIdAndFreelancerId(jobApplicationId, freelancerId)
                .orElseThrow(() -> new Exception("Contract not found"));

        Date now = new Date();
        if (now.before(contract.getContractStartDate())) {
            contract.setContractStatus("upcoming");
        } else {
            contract.setContractStatus("active");
        }

        EmployerProfile employer = employerProfileRepository.findByEmployer_EmployerUserId(contract.getEmployerId())
                .orElseThrow(() -> new RuntimeException("Employer not found"));

        String employerEmail = employer.getEmail();

        sendAcceptEmailToEmployer(employerEmail, contract);

        contractRepository.save(contract);
    }

    public String getContractStatus(String jobApplicationId) {
        return contractRepository.findByJobApplicationId(jobApplicationId)
                .map(Contract::getContractStatus)
                .orElse("not_found");
    }


    public Map<String, String> getContractStatuses(List<String> jobApplicationIds) {
        Map<String, String> result = new HashMap<>();

        for (String id : jobApplicationIds) {
            String status = getContractStatus(id); // reuse single method
            result.put(id, status);
        }

        return result;
    }

    public int countEmployerActiveContracts(String employerId) {
        return contractRepository.countByEmployerIdAndContractStatusIn(
            employerId,
            List.of("pending", "upcoming", "active")
        );
    }

    public int countActiveContractsByFreelancer(String freelancerId) {
        return contractRepository.countFreelancerEligibleContracts(freelancerId);
    }

    public Contract cancelContract(int contractId, String reason) {
        Optional<Contract> contractOpt = contractRepository.findById(contractId);
        if (contractOpt.isEmpty()) {
            throw new RuntimeException("Contract not found with id: " + contractId);
        }

        Contract contract = contractOpt.get();
        contract.setCancellationReason(reason);
        return contractRepository.save(contract);
    }

    public boolean hasCancellationReason(int contractId) {
        return contractRepository.findById(contractId)
                .map(contract -> contract.getCancellationReason() != null && !contract.getCancellationReason().isEmpty())
                .orElse(false);
    }

    
    public boolean isCompletedOrCancelled(String contractId) {
        return contractRepository.findById(Integer.parseInt(contractId))
                .map(contract -> {
                    String status = contract.getContractStatus();
                    return "completed".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status);
                })
                .orElse(false); // return false if not found
    }

    public void completeContractWithFeedback(int contractId, FreelancerFeedbackDTO feedbackDTO) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        contract.setContractStatus("completed");
        contractRepository.save(contract);

        FreelancerFeedback feedback = FreelancerFeedback.builder()
                .rating(feedbackDTO.getRating())
                .feedback(feedbackDTO.getFeedback())
                .employerId(feedbackDTO.getEmployerId())
                .freelancerId(feedbackDTO.getFreelancerId())
                .jobId(feedbackDTO.getJobId())
                .contractId(feedbackDTO.getContractId())
                .build();

        feedbackService.saveFeedback(feedback);
    }


    public void freelancerSendEmployerFeedback(int contractId, EmployerFeedbackDTO feedbackDTO) {

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        contract.setFreelancerFeedback(true);
        contractRepository.save(contract);

        EmployerFeedback feedback = EmployerFeedback.builder()
                .rating(feedbackDTO.getRating())
                .feedback(feedbackDTO.getFeedback())
                .employerId(feedbackDTO.getEmployerId())
                .freelancerId(feedbackDTO.getFreelancerId())
                .jobId(feedbackDTO.getJobId())
                .contractId(feedbackDTO.getContractId())
                .build();

        employerFeedbackService.saveEmployerFeedback(feedback);
    }
}
