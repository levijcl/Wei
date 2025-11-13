package com.wei.orchestrator.observation.application.command;

public class PollInventorySnapshotCommand {
    private String observerId;

    public PollInventorySnapshotCommand() {}

    public PollInventorySnapshotCommand(String observerId) {
        this.observerId = observerId;
    }

    public String getObserverId() {
        return observerId;
    }

    public void setObserverId(String observerId) {
        this.observerId = observerId;
    }
}
