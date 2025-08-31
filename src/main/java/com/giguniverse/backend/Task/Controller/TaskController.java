package com.giguniverse.backend.Task.Controller;

import com.giguniverse.backend.JobPost.ContractHandling.Model.Contract;
import com.giguniverse.backend.Task.Model.Task;
import com.giguniverse.backend.Task.Model.TaskRequestDTO;
import com.giguniverse.backend.Task.Model.TaskWithFilesDTO;
import com.giguniverse.backend.Task.Service.TaskService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @PostMapping("/create")
    public ResponseEntity<Task> createTask(@RequestBody TaskRequestDTO taskRequest) {
        Task createdTask = taskService.createTask(taskRequest);
        return ResponseEntity.ok(createdTask);
    }

    @GetMapping("/employer-view")
    public List<Task> getTasks(
        @RequestParam String employerId,
        @RequestParam String freelancerId,
        @RequestParam String contractId) {
    return taskService.getTasksByContract(employerId, freelancerId, contractId);
    }

    @GetMapping("/freelancer-view")
    public List<Task> getFreelancerTasks(
        @RequestParam String employerId,
        @RequestParam String freelancerId,
        @RequestParam String contractId) {
    return taskService.getTasksByContract(employerId, freelancerId, contractId);
    }

    @GetMapping("/get-end-date")
    public ResponseEntity<?> getContractEndDate(@RequestParam String contractId) {
        Optional<Contract> contractOpt = taskService.getContractById(contractId);
        if (contractOpt.isPresent()) {
            return ResponseEntity.ok(contractOpt.get());
        } else {
            return ResponseEntity.status(404).body("Contract not found");
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<Task> submitTask(
            @RequestParam int taskId,
            @RequestParam String submissionNote,
            @RequestParam(required = false) List<MultipartFile> files
    ) throws IOException {
        Task updatedTask = taskService.submitTask(taskId, submissionNote, files);
        return ResponseEntity.ok(updatedTask);
    }

    @GetMapping("/get-task-file-data")
    public List<TaskWithFilesDTO> getNewTasks(
            @RequestParam String employerId,
            @RequestParam String freelancerId,
            @RequestParam String contractId
    ) {
        return taskService.getTasksWithFiles(employerId, freelancerId, contractId);
    }

    @PostMapping("/update-submission")
    public ResponseEntity<Task> updateSubmission(
            @RequestParam int taskId,
            @RequestParam String submissionNote,
            @RequestParam(required = false) List<MultipartFile> files) throws IOException {

        Task updatedTask = taskService.updateSubmission(taskId, submissionNote, files);
        return ResponseEntity.ok(updatedTask);
    }

}
