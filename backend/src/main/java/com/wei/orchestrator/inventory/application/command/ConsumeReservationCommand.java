package com.wei.orchestrator.inventory.application.command;

public class ConsumeReservationCommand {
    private final String transactionId;
    private final String externalReservationId;
    private final String sourceReferenceId;

    public ConsumeReservationCommand(
            String transactionId, String externalReservationId, String sourceReferenceId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or blank");
        }
        if (externalReservationId == null || externalReservationId.isBlank()) {
            throw new IllegalArgumentException("External reservation ID cannot be null or blank");
        }
        if (sourceReferenceId == null || sourceReferenceId.isBlank()) {
            throw new IllegalArgumentException("Source reference ID cannot be null or blank");
        }

        this.transactionId = transactionId;
        this.externalReservationId = externalReservationId;
        this.sourceReferenceId = sourceReferenceId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getExternalReservationId() {
        return externalReservationId;
    }

    public String getSourceReferenceId() {
        return sourceReferenceId;
    }
}
