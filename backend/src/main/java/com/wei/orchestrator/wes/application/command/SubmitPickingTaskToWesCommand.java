package com.wei.orchestrator.wes.application.command;

public class SubmitPickingTaskToWesCommand {

    private final String taskId;

    public SubmitPickingTaskToWesCommand(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }
}
