package com.wei.orchestrator.observation.application.command;

public class PollWesTaskStatusCommand {
    private String observerId;

    public PollWesTaskStatusCommand() {}

    public PollWesTaskStatusCommand(String observerId) {
        this.observerId = observerId;
    }

    public String getObserverId() {
        return observerId;
    }

    public void setObserverId(String observerId) {
        this.observerId = observerId;
    }
}
