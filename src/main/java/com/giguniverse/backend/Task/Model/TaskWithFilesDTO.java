package com.giguniverse.backend.Task.Model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskWithFilesDTO {
    private Task task;
    private List<TaskFileDocument.FileEntry> files;
}