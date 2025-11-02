package com.wei.orchestrator.wes.application.command;

public class MarkTaskCompletedCommand {

    private final String taskId;

    public MarkTaskCompletedCommand(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }
}
