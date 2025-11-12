package com.wei.orchestrator.inventory.infrastructure.mapper;

import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.model.valueobject.ExternalReservationId;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionLine;
import com.wei.orchestrator.inventory.domain.model.valueobject.WarehouseLocation;
import com.wei.orchestrator.inventory.infrastructure.persistence.InventoryTransactionEntity;
import com.wei.orchestrator.inventory.infrastructure.persistence.TransactionLineEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InventoryTransactionMapper {

    public static InventoryTransactionEntity toEntity(InventoryTransaction domain) {
        InventoryTransactionEntity entity = new InventoryTransactionEntity();
        entity.setTransactionId(domain.getTransactionId());
        entity.setType(domain.getType());
        entity.setStatus(domain.getStatus());
        entity.setSource(domain.getSource());
        entity.setSourceReferenceId(domain.getSourceReferenceId());

        if (domain.getWarehouseLocation() != null) {
            entity.setWarehouseId(domain.getWarehouseLocation().getWarehouseId());
            entity.setZone(domain.getWarehouseLocation().getZone());
        }

        if (domain.getExternalReservationId() != null) {
            entity.setExternalReservationId(domain.getExternalReservationId().getValue());
        }

        entity.setRelatedTransactionId(domain.getRelatedTransactionId());
        entity.setFailureReason(domain.getFailureReason());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setCompletedAt(domain.getCompletedAt());

        return entity;
    }

    public static InventoryTransaction toDomain(
            InventoryTransactionEntity entity, List<TransactionLineEntity> lineEntities) {

        InventoryTransaction domain = new InventoryTransaction();
        domain.setTransactionId(entity.getTransactionId());
        domain.setType(entity.getType());
        domain.setStatus(entity.getStatus());
        domain.setSource(entity.getSource());
        domain.setSourceReferenceId(entity.getSourceReferenceId());

        if (entity.getWarehouseId() != null) {
            domain.setWarehouseLocation(
                    WarehouseLocation.of(entity.getWarehouseId(), entity.getZone()));
        }

        if (lineEntities != null && !lineEntities.isEmpty()) {
            List<TransactionLine> lines =
                    lineEntities.stream()
                            .map(
                                    lineEntity ->
                                            TransactionLine.of(
                                                    lineEntity.getSku(), lineEntity.getQuantity()))
                            .collect(Collectors.toList());
            domain.setTransactionLines(lines);
        }

        if (entity.getExternalReservationId() != null) {
            domain.setExternalReservationId(
                    ExternalReservationId.of(entity.getExternalReservationId()));
        }

        domain.setRelatedTransactionId(entity.getRelatedTransactionId());
        domain.setFailureReason(entity.getFailureReason());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setCompletedAt(entity.getCompletedAt());

        return domain;
    }

    public static List<TransactionLineEntity> toTransactionLineEntities(
            String transactionId, List<TransactionLine> lines) {

        if (lines == null) {
            return new ArrayList<>();
        }

        return lines.stream()
                .map(
                        line -> {
                            TransactionLineEntity entity = new TransactionLineEntity();
                            entity.setTransactionId(transactionId);
                            entity.setSku(line.getSku());
                            entity.setQuantity(line.getQuantity());
                            return entity;
                        })
                .collect(Collectors.toList());
    }
}
