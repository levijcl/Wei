package com.wei.orchestrator.inventory.application.dto;

public class InventoryOperationResultDto {

    private final boolean success;
    private final String transactionId;
    private final String errorMessage;

    private InventoryOperationResultDto(
            boolean success, String transactionId, String errorMessage) {
        this.success = success;
        this.transactionId = transactionId;
        this.errorMessage = errorMessage;
    }

    public static InventoryOperationResultDto success(String transactionId) {
        return new InventoryOperationResultDto(true, transactionId, null);
    }

    public static InventoryOperationResultDto successVoid() {
        return new InventoryOperationResultDto(true, null, null);
    }

    public static InventoryOperationResultDto failure(String errorMessage) {
        return new InventoryOperationResultDto(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
