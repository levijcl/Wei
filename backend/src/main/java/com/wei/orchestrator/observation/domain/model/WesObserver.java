package com.wei.orchestrator.observation.domain.model;

import com.wei.orchestrator.observation.domain.event.WesTaskDiscoveredEvent;
import com.wei.orchestrator.observation.domain.event.WesTaskStatusUpdatedEvent;
import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import com.wei.orchestrator.observation.domain.model.valueobject.TaskEndpoint;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.domain.port.WesPort;
import com.wei.orchestrator.wes.infrastructure.adapter.dto.WesTaskDto;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WesObserver {
    private String observerId;
    private TaskEndpoint taskEndpoint;
    private PollingInterval pollingInterval;
    private LocalDateTime lastPolledTimestamp;
    private boolean active;
    private final List<Object> domainEvents;

    public WesObserver() {
        this.active = true;
        this.domainEvents = new ArrayList<>();
    }

    public WesObserver(
            String observerId, TaskEndpoint taskEndpoint, PollingInterval pollingInterval) {
        if (observerId == null || observerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Observer ID cannot be null or empty");
        }
        if (taskEndpoint == null) {
            throw new IllegalArgumentException("Task endpoint cannot be null");
        }
        if (pollingInterval == null) {
            throw new IllegalArgumentException("Polling interval cannot be null");
        }
        this.observerId = observerId;
        this.taskEndpoint = taskEndpoint;
        this.pollingInterval = pollingInterval;
        this.lastPolledTimestamp = null;
        this.active = true;
        this.domainEvents = new ArrayList<>();
    }

    public void pollWesTaskStatus(WesPort wesPort, List<PickingTask> allPickingTasks) {
        if (!this.shouldPoll()) {
            return;
        }
        List<WesTaskDto> externalWesTasks = wesPort.pollAllTasks();

        this.lastPolledTimestamp = LocalDateTime.now();
        List<String> existingWesTaskIds =
                allPickingTasks.stream()
                        .map(PickingTask::getWesTaskId)
                        .map(WesTaskId::getValue)
                        .toList();

        for (WesTaskDto externalWesTask : externalWesTasks) {
            String wesTaskId = externalWesTask.getTaskId();

            if (!existingWesTaskIds.contains(wesTaskId)) {
                WesTaskDiscoveredEvent event = new WesTaskDiscoveredEvent(externalWesTask);
                this.domainEvents.add(event);
            } else {
                PickingTask currentTask =
                        allPickingTasks.stream()
                                .filter(p -> p.getWesTaskId().getValue().equals(wesTaskId))
                                .findFirst()
                                .orElseThrow(
                                        () -> new RuntimeException("Can not find by wes task id"));
                TaskStatus currentStatus = currentTask.getStatus();
                TaskStatus newStatus = TaskStatus.valueOf(externalWesTask.getStatus());

                if (currentStatus != null && !currentStatus.equals(newStatus)) {
                    WesTaskStatusUpdatedEvent event =
                            new WesTaskStatusUpdatedEvent(currentTask.getTaskId(), newStatus);
                    this.domainEvents.add(event);
                }
            }
        }
    }

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean shouldPoll() {
        if (!active) {
            return false;
        }
        if (lastPolledTimestamp == null) {
            return true;
        }
        LocalDateTime nextPollTime = lastPolledTimestamp.plusSeconds(pollingInterval.getSeconds());
        return LocalDateTime.now().isAfter(nextPollTime);
    }

    public String getObserverId() {
        return observerId;
    }

    public void setObserverId(String observerId) {
        this.observerId = observerId;
    }

    public TaskEndpoint getTaskEndpoint() {
        return taskEndpoint;
    }

    public void setTaskEndpoint(TaskEndpoint taskEndpoint) {
        this.taskEndpoint = taskEndpoint;
    }

    public PollingInterval getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(PollingInterval pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public LocalDateTime getLastPolledTimestamp() {
        return lastPolledTimestamp;
    }

    public void setLastPolledTimestamp(LocalDateTime lastPolledTimestamp) {
        this.lastPolledTimestamp = lastPolledTimestamp;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
