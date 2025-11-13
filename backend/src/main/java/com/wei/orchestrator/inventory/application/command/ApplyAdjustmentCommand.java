package com.wei.orchestrator.inventory.application.command;

public class ApplyAdjustmentCommand {
    private final String adjustmentId;

    public ApplyAdjustmentCommand(String adjustmentId) {
        if (adjustmentId == null || adjustmentId.isBlank()) {
            throw new IllegalArgumentException("Adjustment ID cannot be null or blank");
        }

        this.adjustmentId = adjustmentId;
    }

    public String getAdjustmentId() {
        return adjustmentId;
    }
}
