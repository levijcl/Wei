package com.wei.orchestrator.wes.application.dto;

public class WesOperationResultDto {
    private final boolean success;
    private final String taskId;
    private final String errorMessage;

    public WesOperationResultDto(boolean success, String taskId, String errorMessage) {
        this.success = success;
        this.taskId = taskId;
        this.errorMessage = errorMessage;
    }

    public static WesOperationResultDto success(String transactionId) {
        return new WesOperationResultDto(true, transactionId, null);
    }

    public static WesOperationResultDto successVoid() {
        return new WesOperationResultDto(true, null, null);
    }

    public static WesOperationResultDto failure(String errorMessage) {
        return new WesOperationResultDto(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getTaskId() {
        return this.taskId;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }
}
