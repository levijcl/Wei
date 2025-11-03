package com.wei.orchestrator.wes.application.command;

public class MarkTaskFailedCommand {

    private final String taskId;
    private final String reason;

    public MarkTaskFailedCommand(String taskId, String reason) {
        this.taskId = taskId;
        this.reason = reason;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getReason() {
        return reason;
    }
}
