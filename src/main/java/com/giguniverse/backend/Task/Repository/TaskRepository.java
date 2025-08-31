package com.giguniverse.backend.Task.Repository;

import com.giguniverse.backend.Task.Model.Task;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {
    List<Task> findByEmployerIdAndFreelancerIdAndContractId(String employerId, String freelancerId, String contractId);

    void deleteByTaskId(Long taskId);
}