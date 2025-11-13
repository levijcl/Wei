package com.wei.orchestrator.inventory.infrastructure.mapper;

import com.wei.orchestrator.inventory.domain.model.InventoryAdjustment;
import com.wei.orchestrator.inventory.domain.model.valueobject.DiscrepancyLog;
import com.wei.orchestrator.inventory.infrastructure.persistence.DiscrepancyLogEntity;
import com.wei.orchestrator.inventory.infrastructure.persistence.InventoryAdjustmentEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InventoryAdjustmentMapper {

    public static InventoryAdjustmentEntity toEntity(InventoryAdjustment domain) {
        InventoryAdjustmentEntity entity = new InventoryAdjustmentEntity();
        entity.setAdjustmentId(domain.getAdjustmentId());
        entity.setStatus(domain.getStatus());
        entity.setAppliedTransactionId(domain.getAppliedTransactionId());
        entity.setFailureReason(domain.getFailureReason());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setProcessedAt(domain.getProcessedAt());

        return entity;
    }

    public static InventoryAdjustment toDomain(
            InventoryAdjustmentEntity entity, List<DiscrepancyLogEntity> logEntities) {

        InventoryAdjustment domain = new InventoryAdjustment();
        domain.setAdjustmentId(entity.getAdjustmentId());
        domain.setStatus(entity.getStatus());
        domain.setAppliedTransactionId(entity.getAppliedTransactionId());
        domain.setFailureReason(entity.getFailureReason());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setProcessedAt(entity.getProcessedAt());

        if (logEntities != null && !logEntities.isEmpty()) {
            List<DiscrepancyLog> logs =
                    logEntities.stream()
                            .map(
                                    logEntity ->
                                            DiscrepancyLog.of(
                                                    logEntity.getSku(),
                                                    logEntity.getWarehouseId(),
                                                    logEntity.getExpectedQuantity(),
                                                    logEntity.getActualQuantity()))
                            .collect(Collectors.toList());
            domain.setDiscrepancyLogs(logs);
        }

        return domain;
    }

    public static List<DiscrepancyLogEntity> toDiscrepancyLogEntities(
            InventoryAdjustmentEntity adjustmentEntity, List<DiscrepancyLog> logs) {

        if (logs == null) {
            return new ArrayList<>();
        }

        return logs.stream()
                .map(
                        log -> {
                            DiscrepancyLogEntity entity = new DiscrepancyLogEntity();
                            entity.setAdjustment(adjustmentEntity);
                            entity.setSku(log.getSku());
                            entity.setWarehouseId(log.getWarehouseId());
                            entity.setExpectedQuantity(log.getExpectedQuantity());
                            entity.setActualQuantity(log.getActualQuantity());
                            entity.setDifference(log.getDifference());
                            entity.setDetectedAt(log.getDetectedAt());
                            return entity;
                        })
                .collect(Collectors.toList());
    }
}
