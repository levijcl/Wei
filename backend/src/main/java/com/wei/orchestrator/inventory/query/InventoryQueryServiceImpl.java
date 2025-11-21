package com.wei.orchestrator.inventory.query;

import com.wei.orchestrator.inventory.infrastructure.persistence.InventoryTransactionEntity;
import com.wei.orchestrator.inventory.infrastructure.persistence.TransactionLineEntity;
import com.wei.orchestrator.inventory.query.dto.InventoryTransactionDetailDto;
import com.wei.orchestrator.inventory.query.dto.TransactionLineDto;
import com.wei.orchestrator.inventory.query.dto.WarehouseLocationDto;
import com.wei.orchestrator.inventory.query.infrastructure.InventoryTransactionQueryRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InventoryQueryServiceImpl implements InventoryQueryService {

    private final InventoryTransactionQueryRepository queryRepository;

    public InventoryQueryServiceImpl(InventoryTransactionQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    @Override
    public InventoryTransactionDetailDto getTransactionById(String transactionId) {
        InventoryTransactionEntity entity =
                queryRepository
                        .findById(transactionId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Transaction not found: " + transactionId));

        return mapToDetailDto(entity);
    }

    private InventoryTransactionDetailDto mapToDetailDto(InventoryTransactionEntity entity) {
        WarehouseLocationDto warehouseLocation =
                new WarehouseLocationDto(entity.getWarehouseId(), entity.getZone());

        List<TransactionLineDto> transactionLines =
                entity.getTransactionLines().stream()
                        .map(this::mapToTransactionLineDto)
                        .collect(Collectors.toList());

        return new InventoryTransactionDetailDto(
                entity.getTransactionId(),
                entity.getType() != null ? entity.getType().name() : null,
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getSource() != null ? entity.getSource().name() : null,
                entity.getSourceReferenceId(),
                warehouseLocation,
                transactionLines,
                entity.getExternalReservationId(),
                entity.getRelatedTransactionId(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getUpdatedAt());
    }

    private TransactionLineDto mapToTransactionLineDto(TransactionLineEntity entity) {
        return new TransactionLineDto(entity.getId(), entity.getSku(), entity.getQuantity());
    }
}
