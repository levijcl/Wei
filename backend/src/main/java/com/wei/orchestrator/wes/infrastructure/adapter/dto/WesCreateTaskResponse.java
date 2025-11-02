package com.wei.orchestrator.wes.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WesCreateTaskResponse {
    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("message")
    private String message;

    public WesCreateTaskResponse() {}

    public WesCreateTaskResponse(String taskId, String message) {
        this.taskId = taskId;
        this.message = message;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
