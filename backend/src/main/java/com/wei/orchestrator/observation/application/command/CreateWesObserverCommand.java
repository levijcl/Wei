package com.wei.orchestrator.observation.application.command;

public class CreateWesObserverCommand {
    private String observerId;
    private String taskEndpointUrl;
    private String authToken;
    private int pollingIntervalSeconds;

    public CreateWesObserverCommand() {}

    public CreateWesObserverCommand(
            String observerId,
            String taskEndpointUrl,
            String authToken,
            int pollingIntervalSeconds) {
        this.observerId = observerId;
        this.taskEndpointUrl = taskEndpointUrl;
        this.authToken = authToken;
        this.pollingIntervalSeconds = pollingIntervalSeconds;
    }

    public String getObserverId() {
        return observerId;
    }

    public void setObserverId(String observerId) {
        this.observerId = observerId;
    }

    public String getTaskEndpointUrl() {
        return taskEndpointUrl;
    }

    public void setTaskEndpointUrl(String taskEndpointUrl) {
        this.taskEndpointUrl = taskEndpointUrl;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public int getPollingIntervalSeconds() {
        return pollingIntervalSeconds;
    }

    public void setPollingIntervalSeconds(int pollingIntervalSeconds) {
        this.pollingIntervalSeconds = pollingIntervalSeconds;
    }
}
