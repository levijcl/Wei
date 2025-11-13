package com.wei.orchestrator.observation.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_observers")
public class InventoryObserverEntity {
    @Id
    @Column(name = "observer_id")
    private String observerId;

    @Column(name = "threshold_percent", nullable = false)
    private Double thresholdPercent;

    @Column(name = "check_frequency", nullable = false)
    private Integer checkFrequency;

    @Column(name = "polling_interval_seconds", nullable = false)
    private Integer pollingIntervalSeconds;

    @Column(name = "last_polled_timestamp")
    private LocalDateTime lastPolledTimestamp;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public InventoryObserverEntity() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getObserverId() {
        return observerId;
    }

    public void setObserverId(String observerId) {
        this.observerId = observerId;
    }

    public Double getThresholdPercent() {
        return thresholdPercent;
    }

    public void setThresholdPercent(Double thresholdPercent) {
        this.thresholdPercent = thresholdPercent;
    }

    public Integer getCheckFrequency() {
        return checkFrequency;
    }

    public void setCheckFrequency(Integer checkFrequency) {
        this.checkFrequency = checkFrequency;
    }

    public Integer getPollingIntervalSeconds() {
        return pollingIntervalSeconds;
    }

    public void setPollingIntervalSeconds(Integer pollingIntervalSeconds) {
        this.pollingIntervalSeconds = pollingIntervalSeconds;
    }

    public LocalDateTime getLastPolledTimestamp() {
        return lastPolledTimestamp;
    }

    public void setLastPolledTimestamp(LocalDateTime lastPolledTimestamp) {
        this.lastPolledTimestamp = lastPolledTimestamp;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
