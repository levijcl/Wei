package com.wei.orchestrator.wes.infrastructure.mapper;

import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.infrastructure.persistence.PickingTaskEntity;
import com.wei.orchestrator.wes.infrastructure.persistence.TaskItemEntity;
import java.util.List;
import java.util.stream.Collectors;

public class PickingTaskMapper {

    public static PickingTaskEntity toEntity(PickingTask domain) {
        if (domain == null) {
            return null;
        }

        PickingTaskEntity entity = new PickingTaskEntity();
        entity.setTaskId(domain.getTaskId());
        entity.setWesTaskId(
                domain.getWesTaskId() != null ? domain.getWesTaskId().getValue() : null);
        entity.setOrderId(domain.getOrderId());
        entity.setOrigin(domain.getOrigin());
        entity.setPriority(domain.getPriority());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setSubmittedAt(domain.getSubmittedAt());
        entity.setCompletedAt(domain.getCompletedAt());
        entity.setCanceledAt(domain.getCanceledAt());
        entity.setFailureReason(domain.getFailureReason());

        return entity;
    }

    public static PickingTask toDomain(
            PickingTaskEntity entity, List<TaskItemEntity> itemEntities) {
        if (entity == null) {
            return null;
        }

        PickingTask domain = new PickingTask();
        domain.setTaskId(entity.getTaskId());
        domain.setWesTaskId(
                entity.getWesTaskId() != null ? WesTaskId.of(entity.getWesTaskId()) : null);
        domain.setOrderId(entity.getOrderId());
        domain.setOrigin(entity.getOrigin());
        domain.setPriority(entity.getPriority());
        domain.setStatus(entity.getStatus());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setSubmittedAt(entity.getSubmittedAt());
        domain.setCompletedAt(entity.getCompletedAt());
        domain.setCanceledAt(entity.getCanceledAt());
        domain.setFailureReason(entity.getFailureReason());

        if (itemEntities != null) {
            List<TaskItem> items =
                    itemEntities.stream()
                            .map(
                                    itemEntity ->
                                            TaskItem.of(
                                                    itemEntity.getSku(),
                                                    itemEntity.getQuantity(),
                                                    itemEntity.getLocation()))
                            .collect(Collectors.toList());
            domain.setItems(items);
        }

        return domain;
    }

    public static List<TaskItemEntity> toTaskItemEntities(String taskId, List<TaskItem> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(
                        item ->
                                new TaskItemEntity(
                                        taskId,
                                        item.getSku(),
                                        item.getQuantity(),
                                        item.getLocation()))
                .collect(Collectors.toList());
    }
}
