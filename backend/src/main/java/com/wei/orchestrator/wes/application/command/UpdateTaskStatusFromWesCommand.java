package com.wei.orchestrator.wes.application.command;

import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;

public class UpdateTaskStatusFromWesCommand {

    private final String taskId;
    private final TaskStatus status;

    public UpdateTaskStatusFromWesCommand(String taskId, TaskStatus status) {
        this.taskId = taskId;
        this.status = status;
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskStatus getStatus() {
        return status;
    }
}
