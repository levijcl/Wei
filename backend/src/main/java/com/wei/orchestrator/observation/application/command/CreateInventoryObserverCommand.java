package com.wei.orchestrator.observation.application.command;

public class CreateInventoryObserverCommand {
    private String observerId;
    private double thresholdPercent;
    private int checkFrequency;
    private int pollingIntervalSeconds;

    public CreateInventoryObserverCommand() {}

    public CreateInventoryObserverCommand(
            String observerId,
            double thresholdPercent,
            int checkFrequency,
            int pollingIntervalSeconds) {
        this.observerId = observerId;
        this.thresholdPercent = thresholdPercent;
        this.checkFrequency = checkFrequency;
        this.pollingIntervalSeconds = pollingIntervalSeconds;
    }

    public String getObserverId() {
        return observerId;
    }

    public void setObserverId(String observerId) {
        this.observerId = observerId;
    }

    public double getThresholdPercent() {
        return thresholdPercent;
    }

    public void setThresholdPercent(double thresholdPercent) {
        this.thresholdPercent = thresholdPercent;
    }

    public int getCheckFrequency() {
        return checkFrequency;
    }

    public void setCheckFrequency(int checkFrequency) {
        this.checkFrequency = checkFrequency;
    }

    public int getPollingIntervalSeconds() {
        return pollingIntervalSeconds;
    }

    public void setPollingIntervalSeconds(int pollingIntervalSeconds) {
        this.pollingIntervalSeconds = pollingIntervalSeconds;
    }
}
