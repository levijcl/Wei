package com.wei.orchestrator.wes.infrastructure.adapter;

import com.wei.orchestrator.wes.domain.exception.*;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.domain.port.WesPort;
import com.wei.orchestrator.wes.infrastructure.adapter.dto.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class WesHttpAdapter implements WesPort {

    private static final Logger logger = LoggerFactory.getLogger(WesHttpAdapter.class);

    private final RestTemplate restTemplate;
    private final String wesBaseUrl;

    public WesHttpAdapter(
            RestTemplate restTemplate, @Value("${wes.api.base-url}") String wesBaseUrl) {
        this.restTemplate = restTemplate;
        this.wesBaseUrl = wesBaseUrl;
    }

    @Override
    public WesTaskId submitPickingTask(PickingTask task) {
        try {
            String url = wesBaseUrl + "/api/tasks";

            List<WesTaskItemDto> items =
                    task.getItems().stream()
                            .map(
                                    item ->
                                            new WesTaskItemDto(
                                                    item.getSku(),
                                                    item.getSku(),
                                                    item.getQuantity(),
                                                    item.getLocation()))
                            .collect(Collectors.toList());

            String defaultWarehouseId = "WH001";
            WesCreateTaskRequest request =
                    new WesCreateTaskRequest(
                            "PICKING",
                            task.getOrderId(),
                            defaultWarehouseId,
                            task.getPriority(),
                            items);

            logger.info(
                    "Submitting picking task to WES: taskId={}, orderId={}",
                    task.getTaskId(),
                    task.getOrderId());

            ResponseEntity<WesCreateTaskResponse> response =
                    restTemplate.postForEntity(url, request, WesCreateTaskResponse.class);

            if (response.getBody() == null || response.getBody().getTaskId() == null) {
                throw new WesSubmissionException("WES API returned null task ID");
            }

            String wesTaskId = response.getBody().getTaskId();
            logger.info(
                    "Successfully submitted picking task to WES: taskId={}, wesTaskId={}",
                    task.getTaskId(),
                    wesTaskId);

            return WesTaskId.of(wesTaskId);

        } catch (HttpClientErrorException.NotFound e) {
            logger.error("WES endpoint not available: taskId={}", task.getTaskId(), e);
            throw new WesSubmissionException("WES endpoint not available", e);

        } catch (HttpServerErrorException e) {
            logger.error("WES server error during task submission: taskId={}", task.getTaskId(), e);
            throw new WesSubmissionException("WES server error", e);

        } catch (ResourceAccessException e) {
            logger.error("WES communication timeout: taskId={}", task.getTaskId(), e);
            throw new WesTimeoutException("WES communication timeout", e);

        } catch (RestClientException e) {
            logger.error("Failed to submit picking task to WES: taskId={}", task.getTaskId(), e);
            throw new WesSubmissionException("Failed to submit task to WES", e);
        }
    }

    @Override
    public Optional<TaskStatus> getTaskStatus(WesTaskId wesTaskId) {
        try {
            String url = wesBaseUrl + "/api/tasks/" + wesTaskId.getValue();

            logger.debug("Fetching task status from WES: wesTaskId={}", wesTaskId.getValue());

            ResponseEntity<WesTaskDto> response = restTemplate.getForEntity(url, WesTaskDto.class);

            if (response.getBody() == null) {
                logger.warn(
                        "WES API returned null body for task: wesTaskId={}", wesTaskId.getValue());
                return Optional.empty();
            }

            String wesStatus = response.getBody().getStatus();
            TaskStatus taskStatus = mapWesStatusToTaskStatus(wesStatus);

            logger.debug(
                    "Fetched task status from WES: wesTaskId={}, status={}",
                    wesTaskId.getValue(),
                    taskStatus);

            return Optional.of(taskStatus);

        } catch (HttpClientErrorException.NotFound e) {
            logger.error("WES task not found: wesTaskId={}", wesTaskId.getValue(), e);
            throw new WesTaskNotFoundException(wesTaskId, e);

        } catch (HttpServerErrorException e) {
            logger.error(
                    "WES server error during status fetch: wesTaskId={}", wesTaskId.getValue(), e);
            throw new WesOperationException("WES server error while fetching task status", e);

        } catch (ResourceAccessException e) {
            logger.error(
                    "WES communication timeout during status fetch: wesTaskId={}",
                    wesTaskId.getValue(),
                    e);
            throw new WesTimeoutException(
                    "WES communication timeout while fetching task status", e);

        } catch (RestClientException e) {
            logger.error(
                    "Failed to fetch task status from WES: wesTaskId={}", wesTaskId.getValue(), e);
            throw new WesOperationException("Failed to fetch task status from WES", e);
        }
    }

    @Override
    public void updateTaskPriority(WesTaskId wesTaskId, int priority) {
        try {
            String url = wesBaseUrl + "/api/tasks/" + wesTaskId.getValue() + "/priority";

            WesUpdatePriorityRequest request = new WesUpdatePriorityRequest(priority);

            logger.info(
                    "Updating task priority in WES: wesTaskId={}, priority={}",
                    wesTaskId.getValue(),
                    priority);

            restTemplate.put(url, request);

            logger.info(
                    "Successfully updated task priority in WES: wesTaskId={}",
                    wesTaskId.getValue());

        } catch (HttpClientErrorException.NotFound e) {
            logger.error(
                    "WES task not found for priority update: wesTaskId={}",
                    wesTaskId.getValue(),
                    e);
            throw new WesTaskNotFoundException(wesTaskId, e);

        } catch (HttpServerErrorException e) {
            logger.error(
                    "WES server error during priority update: wesTaskId={}, priority={}",
                    wesTaskId.getValue(),
                    priority,
                    e);
            throw new WesPriorityUpdateException(
                    wesTaskId, priority, "WES server error during priority update", e);

        } catch (ResourceAccessException e) {
            logger.error(
                    "WES communication timeout during priority update: wesTaskId={}",
                    wesTaskId.getValue(),
                    e);
            throw new WesTimeoutException(
                    "WES communication timeout while updating task priority", e);

        } catch (RestClientException e) {
            logger.error(
                    "Failed to update task priority in WES: wesTaskId={}", wesTaskId.getValue(), e);
            throw new WesPriorityUpdateException(
                    wesTaskId, priority, "Failed to update task priority in WES", e);
        }
    }

    @Override
    public void cancelTask(WesTaskId wesTaskId) {
        try {
            String url = wesBaseUrl + "/api/tasks/" + wesTaskId.getValue();

            logger.info("Cancelling task in WES: wesTaskId={}", wesTaskId.getValue());

            restTemplate.delete(url);

            logger.info("Successfully cancelled task in WES: wesTaskId={}", wesTaskId.getValue());

        } catch (HttpClientErrorException.NotFound e) {
            logger.error(
                    "WES task not found for cancellation: wesTaskId={}", wesTaskId.getValue(), e);
            throw new WesTaskNotFoundException(wesTaskId, e);

        } catch (HttpServerErrorException e) {
            logger.error(
                    "WES server error during task cancellation: wesTaskId={}",
                    wesTaskId.getValue(),
                    e);
            throw new WesTaskCancellationException(
                    wesTaskId, "WES server error during task cancellation", e);

        } catch (ResourceAccessException e) {
            logger.error(
                    "WES communication timeout during task cancellation: wesTaskId={}",
                    wesTaskId.getValue(),
                    e);
            throw new WesTimeoutException("WES communication timeout while cancelling task", e);

        } catch (RestClientException e) {
            logger.error("Failed to cancel task in WES: wesTaskId={}", wesTaskId.getValue(), e);
            throw new WesTaskCancellationException(wesTaskId, "Failed to cancel task in WES", e);
        }
    }

    @Override
    public List<WesTaskDto> pollAllTasks() {
        try {
            String url = wesBaseUrl + "/api/tasks";

            logger.debug("Polling all tasks from WES");

            ResponseEntity<WesTaskListResponse> response =
                    restTemplate.getForEntity(url, WesTaskListResponse.class);

            if (response.getBody() == null || response.getBody().getTasks() == null) {
                logger.warn("WES API returned null body or tasks for tasks list");
                return List.of();
            }

            List<WesTaskDto> tasks = response.getBody().getTasks();

            logger.debug(
                    "Polled {} tasks from WES (count: {})",
                    tasks.size(),
                    response.getBody().getCount());

            return tasks;

        } catch (HttpClientErrorException.NotFound e) {
            logger.error("WES tasks endpoint not found", e);
            throw new WesOperationException("WES tasks endpoint not found", e);

        } catch (HttpServerErrorException e) {
            logger.error("WES server error during tasks polling", e);
            throw new WesOperationException("WES server error during tasks polling", e);

        } catch (ResourceAccessException e) {
            logger.error("WES communication timeout during tasks polling", e);
            throw new WesTimeoutException("WES communication timeout during tasks polling", e);

        } catch (RestClientException e) {
            logger.error("Failed to poll tasks from WES", e);
            throw new WesOperationException("Failed to poll tasks from WES", e);
        }
    }

    private TaskStatus mapWesStatusToTaskStatus(String wesStatus) {
        if (wesStatus == null) {
            return TaskStatus.PENDING;
        }

        return switch (wesStatus.toUpperCase()) {
            case "PENDING" -> TaskStatus.PENDING;
            case "IN_PROGRESS" -> TaskStatus.IN_PROGRESS;
            case "COMPLETED" -> TaskStatus.COMPLETED;
            case "FAILED" -> TaskStatus.FAILED;
            case "CANCELLED" -> TaskStatus.CANCELED;
            default -> {
                logger.warn("Unknown WES status: {}, defaulting to PENDING", wesStatus);
                yield TaskStatus.PENDING;
            }
        };
    }
}
