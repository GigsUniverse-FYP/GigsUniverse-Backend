package com.giguniverse.backend.Task.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.giguniverse.backend.JobPost.ContractHandling.Model.Contract;
import com.giguniverse.backend.JobPost.ContractHandling.Repository.ContractRepository;
import com.giguniverse.backend.JobPost.CreateJobPost.model.JobPost;
import com.giguniverse.backend.JobPost.CreateJobPost.repository.JobPostRepository;
import com.giguniverse.backend.Profile.Model.EmployerProfile;
import com.giguniverse.backend.Profile.Repository.EmployerProfileRepository;
import com.giguniverse.backend.Profile.Repository.FreelancerProfileRepository;
import com.giguniverse.backend.Task.Model.Task;
import com.giguniverse.backend.Task.Model.TaskFileDocument;
import com.giguniverse.backend.Task.Model.TaskRequestDTO;
import com.giguniverse.backend.Task.Model.TaskWithFilesDTO;
import com.giguniverse.backend.Task.Repository.TaskFileDocumentRepository;
import com.giguniverse.backend.Task.Repository.TaskRepository;
import com.giguniverse.backend.Transaction.Model.Transaction;
import com.giguniverse.backend.Transaction.Repository.TransactionRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EmployerProfileRepository employerProfileRepository;

    @Autowired
    private FreelancerProfileRepository freelancerProfileRepository;

    @Autowired
    private JobPostRepository jobPostRepository; 

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private TaskFileDocumentRepository taskFileDocumentRepository;

    public Task createTask(TaskRequestDTO req) {
        EmployerProfile employer = employerProfileRepository
                .findByEmployer_EmployerUserId(req.getEmployerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Employer not found with id: " + req.getEmployerId()
                ));

        // Convert task pay (frontend sends dollars) -> cents
        long taskPayCents = (long) (Double.parseDouble(req.getTaskTotalPay()) * 100);

        long currentCredits = employer.getAvailableCredits();
        if (currentCredits < taskPayCents) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient credits for employer: " + req.getEmployerId()
            );
        }

        // Deduct credits
        long updatedCredits = currentCredits - taskPayCents;
        employer.setAvailableCredits(updatedCredits);
        employerProfileRepository.save(employer);

        // Build and save task
        Task task = Task.builder()
                .taskName(req.getTaskName())
                .taskInstruction(req.getTaskInstruction())
                .taskSubmission(req.getTaskSubmission())
                .taskHour(req.getTaskHour())
                .taskTotalPay(taskPayCents)  // keep in cents for storage
                .taskStatus("pending")
                .taskCreationDate(new Date())
                .taskDueDate(req.getTaskDueDate())    // already Date in DTO
                .employerId(req.getEmployerId())
                .freelancerId(req.getFreelancerId())
                .jobId(req.getJobId())
                .contractId(req.getContractId())
                .build();

        Task savedTask = taskRepository.save(task);

        // Record transaction (negative because employer pays)
        Transaction transaction = Transaction.builder()
                .employerUserId(req.getEmployerId())
                .stripePaymentIntentId(null)  
                .stripeCheckoutSessionId(null) 
                .amount(-taskPayCents)  // negative for payment
                .currency("usd")
                .status("success")
                .paymentMethodType("credits")
                .paymentType("Task Payment")
                .description("Task creation escrow payment for " + req.getTaskName())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        transactionRepository.save(transaction);

        // Notify freelancer
        notifyFreelancer(savedTask);

        return savedTask;
    }


    @Autowired
    private JavaMailSender mailSender;

    public void notifyFreelancer(Task task) {
        System.out.println("notifyFreelancer called for taskId: " + task.getTaskId() + ", freelancerId: " + task.getFreelancerId());

        freelancerProfileRepository.findByFreelancer_FreelancerUserId(task.getFreelancerId())
            .ifPresentOrElse(freelancer -> {
                System.out.println("Freelancer found: " + freelancer.getFreelancer().getEmail());

                try {
                    JobPost jobPost = jobPostRepository.findById(Integer.parseInt(task.getJobId()))
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Job post not found with id: " + task.getJobId()
                            ));

                    System.out.println("JobPost found: " + jobPost.getJobTitle());

                    String jobTitle = jobPost.getJobTitle();
                    String jobId = String.valueOf(jobPost.getJobPostId());

                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true);

                    String freelancerEmail = freelancer.getFreelancer().getEmail();
                    String freelancerName = freelancer.getFreelancer().getProfile().getFullName();

                    long totalPayCents = task.getTaskTotalPay();
                    double totalPayUsd = totalPayCents / 100.0;
                    String formattedPay = String.format("%.2f", totalPayUsd);

                    String body = "<div style='font-family:Arial,sans-serif;font-size:14px;color:#333;'>"
                        + "<p>Dear " + freelancerName + ",</p>"
                        + "<p>You have been assigned a new <strong>Task</strong> under the following job:</p>"
                        + "<p><strong>Job Title:</strong> " + jobTitle + "</p>"
                        + "<p><strong>Job ID:</strong> " + jobId + "</p>"
                        + "<br>"
                        + "<p><strong>Task Name:</strong> " + task.getTaskName() + "</p>"
                        + "<p><strong>Instructions:</strong> " + task.getTaskInstruction() + "</p>"
                        + "<p><strong>Total Pay:</strong> $" + formattedPay + "</p>"
                        + "<p><strong>Due Date:</strong> " + task.getTaskDueDate() + "</p>"
                        + "<br>"
                        + "<p>Please login to your account to view and submit this task.</p>"
                        + "<br>"
                        + "<p>Regards,<br>GigsUniverse Team</p>"
                        + "</div>";

                    helper.setFrom("admin@gigsuniverse.studio");
                    helper.setTo(freelancerEmail);
                    helper.setSubject("New Task Assigned - GigsUniverse");
                    helper.setText(body, true);

                    System.out.println("Sending email to: " + freelancerEmail);
                    mailSender.send(message);
                    System.out.println("Email sent successfully!");

                } catch (MessagingException e) {
                    System.err.println("Failed to send email to freelancer: " + freelancer.getFreelancer().getEmail());
                    e.printStackTrace();
                } catch (Exception e) {
                    System.err.println("Unexpected error in notifyFreelancer");
                    e.printStackTrace();
                }

            }, () -> System.err.println("Freelancer not found for ID: " + task.getFreelancerId()));
    }


    // getting contract end date
    public Optional<Contract> getContractById(String contractId) {
        return contractRepository.findById(Integer.parseInt(contractId));
    }

    // show task views
    public List<Task> getTasksByContract(String employerId, String freelancerId, String contractId) {
        return taskRepository.findByEmployerIdAndFreelancerIdAndContractId(employerId, freelancerId, contractId);
    }

    public List<TaskWithFilesDTO> getTasksWithFiles(String employerId, String freelancerId, String contractId) {
        List<Task> tasks = taskRepository.findByEmployerIdAndFreelancerIdAndContractId(employerId, freelancerId, contractId);
        List<Integer> taskIds = tasks.stream().map(Task::getTaskId).toList();
        List<TaskFileDocument> fileDocs = taskFileDocumentRepository.findByTaskIdIn(taskIds);

        Map<Integer, List<TaskFileDocument.FileEntry>> taskFilesMap = fileDocs.stream()
                .collect(Collectors.toMap(TaskFileDocument::getTaskId, TaskFileDocument::getFiles));

        return tasks.stream()
                .sorted(Comparator
                        .comparing(Task::getTaskStatus) // simple alphabetical sort by status
                        .thenComparing(task -> {
                            try {
                                return task.getTaskDueDate().toInstant();
                            } catch (Exception e) {
                                return Instant.MAX;
                            }
                        })
                )
                .map(task -> TaskWithFilesDTO.builder()
                        .task(task)
                        .files(taskFilesMap.getOrDefault(task.getTaskId(), Collections.emptyList()))
                        .build())
                .toList();
    }


    // submit and edit task
    public Task submitTask(int taskId, String submissionNote, List<MultipartFile> files) throws IOException {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setTaskStatus("submitted");
        task.setTaskSubmissionNote(submissionNote);
        task.setTaskSubmissionDate(new Date());
        taskRepository.save(task);

        if (files != null && !files.isEmpty()) {
            List<TaskFileDocument.FileEntry> fileEntries = files.stream().map(file -> {
                try {
                    return TaskFileDocument.FileEntry.builder()
                            .fileName(file.getOriginalFilename())
                            .fileType(file.getContentType())
                            .fileSize(file.getSize())
                            .fileData(file.getBytes())
                            .build();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            TaskFileDocument doc = TaskFileDocument.builder()
                    .taskId(taskId)
                    .files(fileEntries)
                    .build();

            taskFileDocumentRepository.save(doc);
        }

        return task;
    }

    public Task updateSubmission(int taskId, String submissionNote, List<MultipartFile> files) throws IOException {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        // delete existing files for this task
        taskFileDocumentRepository.deleteByTaskId(taskId);

        // save new files
        if (files != null && !files.isEmpty()) {
            List<TaskFileDocument.FileEntry> fileEntries = files.stream().map(file -> {
                try {
                    return TaskFileDocument.FileEntry.builder()
                            .fileName(file.getOriginalFilename())
                            .fileType(file.getContentType())
                            .fileSize(file.getSize())
                            .fileData(file.getBytes())
                            .build();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            TaskFileDocument doc = TaskFileDocument.builder()
                    .taskId(taskId)
                    .files(fileEntries)
                    .build();

            taskFileDocumentRepository.save(doc);
        }

        // update task note and status
        task.setTaskSubmissionNote(submissionNote);
        task.setTaskSubmissionDate(new Date());
        task.setTaskStatus("submitted");
        return taskRepository.save(task);
    }

}
