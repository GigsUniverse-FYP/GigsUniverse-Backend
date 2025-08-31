package com.giguniverse.backend.Task.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.giguniverse.backend.Task.Model.TaskFileDocument;

@Repository
public interface TaskFileDocumentRepository extends MongoRepository<TaskFileDocument, String> {
    List<TaskFileDocument> findByTaskIdIn(List<Integer> taskIds);
    void deleteByTaskId(int taskId);
}