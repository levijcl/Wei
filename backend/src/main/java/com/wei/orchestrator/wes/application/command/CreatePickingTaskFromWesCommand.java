package com.wei.orchestrator.wes.application.command;

public class CreatePickingTaskFromWesCommand {
    private String wesTaskId;
    private Integer priority;

    public CreatePickingTaskFromWesCommand() {}

    public CreatePickingTaskFromWesCommand(String wesTaskId, Integer priority) {
        this.wesTaskId = wesTaskId;
        this.priority = priority;
    }

    public String getWesTaskId() {
        return wesTaskId;
    }

    public void setWesTaskId(String wesTaskId) {
        this.wesTaskId = wesTaskId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
