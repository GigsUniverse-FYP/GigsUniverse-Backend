package com.giguniverse.backend.Task.Repository;

import com.giguniverse.backend.Task.Model.Task;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {
    List<Task> findByEmployerIdAndFreelancerIdAndContractId(String employerId, String freelancerId, String contractId);

    void deleteByTaskId(Long taskId);


    List<Task> findByFreelancerIdAndTaskStatusInAndTaskDueDateBetween(
            String freelancerId, List<String> statuses, Date start, Date end
    );

    long countByFreelancerIdAndTaskStatus(String freelancerId, String taskStatus);

    long countByFreelancerIdAndTaskStatusIn(String freelancerId, List<String> statuses);

    List<Task> findTop4ByFreelancerIdAndTaskStatusOrderByTaskDueDateAsc(String freelancerId, String taskStatus);

    List<Task> findByEmployerIdAndTaskStatusIn(String employerId, List<String> statuses);

    List<Task> findByEmployerIdAndTaskStatus(String employerId, String taskStatus);

    List<Task> findByTaskStatus(String status);

    List<Task> findByTaskStatusAndTaskSubmissionDateBetween(
            String status,
            Date start,
            Date end
    );

}