package com.wei.orchestrator.observation.application.command;

public class CreateOrderObserverCommand {
    private String observerId;
    private String jdbcUrl;
    private String username;
    private String password;
    private int pollingIntervalSeconds;

    public CreateOrderObserverCommand() {}

    public CreateOrderObserverCommand(
            String observerId,
            String jdbcUrl,
            String username,
            String password,
            int pollingIntervalSeconds) {
        this.observerId = observerId;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.pollingIntervalSeconds = pollingIntervalSeconds;
    }

    public String getObserverId() {
        return observerId;
    }

    public void setObserverId(String observerId) {
        this.observerId = observerId;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPollingIntervalSeconds() {
        return pollingIntervalSeconds;
    }

    public void setPollingIntervalSeconds(int pollingIntervalSeconds) {
        this.pollingIntervalSeconds = pollingIntervalSeconds;
    }
}
