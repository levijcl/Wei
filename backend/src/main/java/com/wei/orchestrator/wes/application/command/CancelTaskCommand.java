package com.wei.orchestrator.wes.application.command;

public class CancelTaskCommand {
    private String taskId;
    private String reason;

    public CancelTaskCommand() {}

    public CancelTaskCommand(String taskId, String reason) {
        this.taskId = taskId;
        this.reason = reason;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
