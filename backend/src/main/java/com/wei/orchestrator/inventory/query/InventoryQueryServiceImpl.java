package com.wei.orchestrator.inventory.query;

import com.wei.orchestrator.inventory.domain.model.valueobject.AdjustmentStatus;
import com.wei.orchestrator.inventory.infrastructure.persistence.DiscrepancyLogEntity;
import com.wei.orchestrator.inventory.infrastructure.persistence.InventoryAdjustmentEntity;
import com.wei.orchestrator.inventory.infrastructure.persistence.InventoryTransactionEntity;
import com.wei.orchestrator.inventory.infrastructure.persistence.TransactionLineEntity;
import com.wei.orchestrator.inventory.query.dto.DiscrepancyLogDto;
import com.wei.orchestrator.inventory.query.dto.InventoryAdjustmentSummaryDto;
import com.wei.orchestrator.inventory.query.dto.InventoryTransactionDetailDto;
import com.wei.orchestrator.inventory.query.dto.TransactionLineDto;
import com.wei.orchestrator.inventory.query.dto.WarehouseLocationDto;
import com.wei.orchestrator.inventory.query.infrastructure.InventoryAdjustmentQueryRepository;
import com.wei.orchestrator.inventory.query.infrastructure.InventoryTransactionQueryRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InventoryQueryServiceImpl implements InventoryQueryService {

    private final InventoryTransactionQueryRepository queryRepository;
    private final InventoryAdjustmentQueryRepository adjustmentQueryRepository;

    public InventoryQueryServiceImpl(
            InventoryTransactionQueryRepository queryRepository,
            InventoryAdjustmentQueryRepository adjustmentQueryRepository) {
        this.queryRepository = queryRepository;
        this.adjustmentQueryRepository = adjustmentQueryRepository;
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

    @Override
    public Page<InventoryAdjustmentSummaryDto> getAdjustments(
            List<AdjustmentStatus> statuses, Pageable pageable) {
        Page<Object[]> resultPage;

        if (statuses == null || statuses.isEmpty()) {
            resultPage = adjustmentQueryRepository.findAllAdjustmentSummariesNative(pageable);
        } else {
            List<String> statusStrings =
                    statuses.stream().map(Enum::name).collect(Collectors.toList());
            resultPage =
                    adjustmentQueryRepository.findAdjustmentSummariesByStatusNative(
                            statusStrings, pageable);
        }

        List<InventoryAdjustmentSummaryDto> dtos =
                resultPage.getContent().stream()
                        .map(this::mapToAdjustmentSummaryDto)
                        .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, resultPage.getTotalElements());
    }

    @Override
    public List<DiscrepancyLogDto> getDiscrepancyLogsByAdjustmentId(String adjustmentId) {
        InventoryAdjustmentEntity entity =
                adjustmentQueryRepository
                        .findById(adjustmentId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Adjustment not found: " + adjustmentId));

        return entity.getDiscrepancyLogs().stream()
                .map(this::mapToDiscrepancyLogDto)
                .collect(Collectors.toList());
    }

    private InventoryAdjustmentSummaryDto mapToAdjustmentSummaryDto(Object[] row) {
        String adjustmentId = (String) row[0];
        String status = (String) row[1];
        LocalDateTime createdAt =
                row[2] instanceof Timestamp
                        ? ((Timestamp) row[2]).toLocalDateTime()
                        : (LocalDateTime) row[2];
        LocalDateTime processedAt =
                row[3] == null
                        ? null
                        : (row[3] instanceof Timestamp
                                ? ((Timestamp) row[3]).toLocalDateTime()
                                : (LocalDateTime) row[3]);
        Integer discrepancyCount = ((Number) row[4]).intValue();
        Integer totalDifference = ((Number) row[5]).intValue();

        return new InventoryAdjustmentSummaryDto(
                adjustmentId, status, discrepancyCount, totalDifference, createdAt, processedAt);
    }

    private DiscrepancyLogDto mapToDiscrepancyLogDto(DiscrepancyLogEntity entity) {
        return new DiscrepancyLogDto(
                entity.getId(),
                entity.getSku(),
                entity.getWarehouseId(),
                entity.getExpectedQuantity(),
                entity.getActualQuantity(),
                entity.getDifference(),
                entity.getDetectedAt());
    }
}
