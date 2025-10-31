package com.wei.orchestrator.observation.application.command;

public class PollOrderSourceCommand {
    private String observerId;

    public PollOrderSourceCommand() {}

    public PollOrderSourceCommand(String observerId) {
        this.observerId = observerId;
    }

    public String getObserverId() {
        return observerId;
    }

    public void setObserverId(String observerId) {
        this.observerId = observerId;
    }
}
