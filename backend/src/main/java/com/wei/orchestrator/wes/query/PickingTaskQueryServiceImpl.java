package com.wei.orchestrator.wes.query;

import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import com.wei.orchestrator.wes.query.dto.PickingTaskDetailDto;
import com.wei.orchestrator.wes.query.dto.PickingTaskSummaryDto;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PickingTaskQueryServiceImpl implements PickingTaskQueryService {

    private final PickingTaskRepository pickingTaskRepository;

    public PickingTaskQueryServiceImpl(PickingTaskRepository pickingTaskRepository) {
        this.pickingTaskRepository = pickingTaskRepository;
    }

    @Override
    public PickingTaskDetailDto getPickingTask(String taskId) {
        PickingTask task =
                pickingTaskRepository
                        .findById(taskId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Picking task not found: " + taskId));

        return mapToDetailDto(task);
    }

    @Override
    public List<PickingTaskSummaryDto> getPickingTasksByOrderId(String orderId) {
        List<PickingTask> tasks = pickingTaskRepository.findByOrderId(orderId);
        return tasks.stream().map(this::mapToSummaryDto).collect(Collectors.toList());
    }

    private PickingTaskDetailDto mapToDetailDto(PickingTask task) {
        List<PickingTaskDetailDto.TaskItemDto> itemDtos =
                task.getItems().stream().map(this::mapToTaskItemDto).collect(Collectors.toList());

        return new PickingTaskDetailDto(
                task.getTaskId(),
                task.getWesTaskId() != null ? task.getWesTaskId().getValue() : null,
                task.getOrderId(),
                task.getOrigin(),
                task.getPriority(),
                task.getStatus(),
                itemDtos,
                task.getCreatedAt(),
                task.getSubmittedAt(),
                task.getCompletedAt(),
                task.getCanceledAt(),
                task.getFailureReason());
    }

    private PickingTaskSummaryDto mapToSummaryDto(PickingTask task) {
        return new PickingTaskSummaryDto(
                task.getTaskId(),
                task.getWesTaskId() != null ? task.getWesTaskId().getValue() : null,
                task.getOrderId(),
                task.getOrigin(),
                task.getPriority(),
                task.getStatus(),
                task.getItems().size(),
                task.getCreatedAt(),
                task.getCompletedAt());
    }

    private PickingTaskDetailDto.TaskItemDto mapToTaskItemDto(TaskItem item) {
        return new PickingTaskDetailDto.TaskItemDto(
                item.getSku(), item.getQuantity(), item.getLocation());
    }
}
