package com.wei.orchestrator.inventory.application.command;

public class ReleaseReservationCommand {
    private final String transactionId;
    private final String externalReservationId;
    private final String reason;

    public ReleaseReservationCommand(
            String transactionId, String externalReservationId, String reason) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or blank");
        }
        if (externalReservationId == null || externalReservationId.isBlank()) {
            throw new IllegalArgumentException("External reservation ID cannot be null or blank");
        }

        this.transactionId = transactionId;
        this.externalReservationId = externalReservationId;
        this.reason = reason;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getExternalReservationId() {
        return externalReservationId;
    }

    public String getReason() {
        return reason;
    }
}
