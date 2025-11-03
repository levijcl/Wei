package com.wei.orchestrator.wes.application.command;

public class AdjustTaskPriorityCommand {

    private final String taskId;
    private final int newPriority;

    public AdjustTaskPriorityCommand(String taskId, int newPriority) {
        this.taskId = taskId;
        this.newPriority = newPriority;
    }

    public String getTaskId() {
        return taskId;
    }

    public int getNewPriority() {
        return newPriority;
    }
}
