package com.wei.orchestrator.unit.observation.domain.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.observation.domain.event.WesTaskStatusUpdatedEvent;
import com.wei.orchestrator.observation.domain.model.WesObserver;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WesObserverTest {

    @Mock private WesPort wesPort;

    @Test
    void shouldCreateWesObserverWithValidParameters() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);

        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);

        assertNotNull(wesObserver);
        assertEquals("observer-1", wesObserver.getObserverId());
        assertTrue(wesObserver.isActive());
        assertNull(wesObserver.getLastPolledTimestamp());
        assertTrue(wesObserver.getDomainEvents().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenObserverIdIsNull() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new WesObserver(null, taskEndpoint, pollingInterval);
                        });

        assertTrue(exception.getMessage().contains("Observer ID cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenObserverIdIsEmpty() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new WesObserver("", taskEndpoint, pollingInterval);
                        });

        assertTrue(exception.getMessage().contains("Observer ID cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenTaskEndpointIsNull() {
        PollingInterval pollingInterval = new PollingInterval(60);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new WesObserver("observer-1", null, pollingInterval);
                        });

        assertTrue(exception.getMessage().contains("Task endpoint cannot be null"));
    }

    @Test
    void shouldThrowExceptionWhenPollingIntervalIsNull() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new WesObserver("observer-1", taskEndpoint, null);
                        });

        assertTrue(exception.getMessage().contains("Polling interval cannot be null"));
    }

    @Test
    void shouldReturnTrueWhenNeverPolledBefore() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);
        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);

        boolean shouldPoll = wesObserver.shouldPoll();

        assertTrue(shouldPoll);
    }

    @Test
    void shouldReturnFalseWhenPolledRecently() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);
        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);
        wesObserver.setLastPolledTimestamp(LocalDateTime.now());

        boolean shouldPoll = wesObserver.shouldPoll();

        assertFalse(shouldPoll);
    }

    @Test
    void shouldReturnTrueWhenPollingIntervalHasPassed() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);
        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);
        wesObserver.setLastPolledTimestamp(LocalDateTime.now().minusSeconds(61));

        boolean shouldPoll = wesObserver.shouldPoll();

        assertTrue(shouldPoll);
    }

    @Test
    void shouldReturnFalseWhenObserverIsInactive() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);
        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);
        wesObserver.deactivate();

        boolean shouldPoll = wesObserver.shouldPoll();

        assertFalse(shouldPoll);
    }

    @Test
    void shouldNotPollWhenShouldPollReturnsFalse() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);
        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);
        wesObserver.setLastPolledTimestamp(LocalDateTime.now());

        wesObserver.pollWesTaskStatus(wesPort, Collections.emptyList());

        verify(wesPort, never()).pollAllTasks();
        assertTrue(wesObserver.getDomainEvents().isEmpty());
    }

    @Test
    void shouldFetchTasksWhenShouldPollReturnsTrue() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);
        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);
        List<WesTaskDto> mockTasks = createMockWesTasks(2);
        when(wesPort.pollAllTasks()).thenReturn(mockTasks);
        wesObserver.pollWesTaskStatus(wesPort, Collections.emptyList());

        verify(wesPort).pollAllTasks();
        assertNotNull(wesObserver.getLastPolledTimestamp());
    }

    @Test
    void shouldGenerateEventWhenTaskStatusChanges() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);
        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);

        List<WesTaskDto> mockTasks = createMockWesTasks(1);
        mockTasks.get(0).setStatus("COMPLETED");
        when(wesPort.pollAllTasks()).thenReturn(mockTasks);

        wesObserver.pollWesTaskStatus(
                wesPort,
                List.of(createPickingTask("TASK_ID_001", "WES-TASK-001", TaskStatus.IN_PROGRESS)));

        List<Object> domainEvents = wesObserver.getDomainEvents();
        assertEquals(1, domainEvents.size());
        assertInstanceOf(WesTaskStatusUpdatedEvent.class, domainEvents.get(0));

        WesTaskStatusUpdatedEvent event = (WesTaskStatusUpdatedEvent) domainEvents.get(0);
        assertEquals("TASK_ID_001", event.getTaskId());
        assertEquals(TaskStatus.COMPLETED, event.getNewStatus());
    }

    @Test
    void shouldNotGenerateEventWhenStatusIsUnchanged() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);
        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);

        List<WesTaskDto> mockTasks = createMockWesTasks(1);
        mockTasks.get(0).setStatus("IN_PROGRESS");
        when(wesPort.pollAllTasks()).thenReturn(mockTasks);

        wesObserver.pollWesTaskStatus(
                wesPort, List.of(createPickingTask("WES-TASK-001", TaskStatus.IN_PROGRESS)));

        assertTrue(wesObserver.getDomainEvents().isEmpty());
    }

    @Test
    void shouldCollectMultipleEventsWhenMultipleChangesDetected() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);
        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);

        List<WesTaskDto> mockTasks = createMockWesTasks(3);
        mockTasks.get(0).setStatus("COMPLETED");
        mockTasks.get(1).setStatus("FAILED");
        mockTasks.get(2).setStatus("IN_PROGRESS");

        when(wesPort.pollAllTasks()).thenReturn(mockTasks);

        wesObserver.pollWesTaskStatus(
                wesPort,
                List.of(
                        createPickingTask("WES-TASK-001", TaskStatus.IN_PROGRESS),
                        createPickingTask("WES-TASK-002", TaskStatus.IN_PROGRESS),
                        createPickingTask("WES-TASK-003", TaskStatus.PENDING)));

        assertEquals(3, wesObserver.getDomainEvents().size());
        for (Object event : wesObserver.getDomainEvents()) {
            assertInstanceOf(WesTaskStatusUpdatedEvent.class, event);
        }
    }

    @Test
    void shouldNotCollectEventsWhenNoChangesFound() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);
        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);

        when(wesPort.pollAllTasks()).thenReturn(Collections.emptyList());

        wesObserver.pollWesTaskStatus(wesPort, Collections.emptyList());

        assertTrue(wesObserver.getDomainEvents().isEmpty());
    }

    @Test
    void shouldUpdateLastPolledTimestampAfterSuccessfulPoll() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);
        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);
        LocalDateTime beforePoll = LocalDateTime.now();

        List<WesTaskDto> mockTasks = createMockWesTasks(1);
        when(wesPort.pollAllTasks()).thenReturn(mockTasks);

        wesObserver.pollWesTaskStatus(wesPort, Collections.emptyList());

        assertNotNull(wesObserver.getLastPolledTimestamp());
        assertTrue(
                wesObserver.getLastPolledTimestamp().isAfter(beforePoll)
                        || wesObserver.getLastPolledTimestamp().isEqual(beforePoll));
    }

    @Test
    void shouldClearAllDomainEvents() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);
        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);

        List<WesTaskDto> mockTasks = createMockWesTasks(2);
        mockTasks.get(0).setStatus("COMPLETED");
        mockTasks.get(1).setStatus("FAILED");
        when(wesPort.pollAllTasks()).thenReturn(mockTasks);

        wesObserver.pollWesTaskStatus(
                wesPort,
                List.of(
                        createPickingTask("WES-TASK-001", TaskStatus.PENDING),
                        createPickingTask("WES-TASK-001", TaskStatus.PENDING)));

        wesObserver.clearDomainEvents();

        assertTrue(wesObserver.getDomainEvents().isEmpty());
    }

    @Test
    void shouldReturnUnmodifiableListOfDomainEvents() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);
        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);

        List<WesTaskDto> mockTasks = createMockWesTasks(1);
        mockTasks.get(0).setStatus("COMPLETED");
        when(wesPort.pollAllTasks()).thenReturn(mockTasks);

        wesObserver.pollWesTaskStatus(
                wesPort, List.of(createPickingTask("WES-TASK-001", TaskStatus.PENDING)));

        List<Object> events = wesObserver.getDomainEvents();

        assertThrows(UnsupportedOperationException.class, () -> events.add(new Object()));
    }

    @Test
    void shouldActivateObserver() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);
        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);
        wesObserver.deactivate();

        wesObserver.activate();

        assertTrue(wesObserver.isActive());
    }

    @Test
    void shouldDeactivateObserver() {
        TaskEndpoint taskEndpoint = new TaskEndpoint("http://localhost:8080/api", "token123");
        PollingInterval pollingInterval = new PollingInterval(60);
        WesObserver wesObserver = new WesObserver("observer-1", taskEndpoint, pollingInterval);

        wesObserver.deactivate();

        assertFalse(wesObserver.isActive());
    }

    private List<WesTaskDto> createMockWesTasks(int count) {
        List<WesTaskDto> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            WesTaskDto task = new WesTaskDto();
            task.setTaskId("WES-TASK-00" + (i + 1));
            task.setTaskType("PICKING");
            task.setOrderId("ORDER-" + (i + 1));
            task.setWarehouseId("WH001");
            task.setPriority(5);
            task.setStatus("PENDING");
            tasks.add(task);
        }
        return tasks;
    }

    private PickingTask createPickingTask(String taskId, String wesTaskId, TaskStatus taskStatus) {
        var pickingTask = new PickingTask();
        pickingTask.setWesTaskId(WesTaskId.of(wesTaskId));
        pickingTask.setStatus(taskStatus);
        pickingTask.setTaskId(taskId);

        return pickingTask;
    }

    private PickingTask createPickingTask(String wesTaskId, TaskStatus taskStatus) {
        var pickingTask = new PickingTask();
        pickingTask.setWesTaskId(WesTaskId.of(wesTaskId));
        pickingTask.setStatus(taskStatus);

        return pickingTask;
    }
}
