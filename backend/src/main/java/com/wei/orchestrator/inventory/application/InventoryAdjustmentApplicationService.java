package com.wei.orchestrator.inventory.application;

import com.wei.orchestrator.inventory.application.command.ApplyAdjustmentCommand;
import com.wei.orchestrator.inventory.application.command.DetectDiscrepancyCommand;
import com.wei.orchestrator.inventory.application.translator.WesTranslator;
import com.wei.orchestrator.inventory.domain.model.InventoryAdjustment;
import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.model.valueobject.DiscrepancyLog;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionLine;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionSource;
import com.wei.orchestrator.inventory.domain.port.InventoryPort;
import com.wei.orchestrator.inventory.domain.repository.InventoryAdjustmentRepository;
import com.wei.orchestrator.inventory.domain.repository.InventoryTransactionRepository;
import com.wei.orchestrator.observation.domain.model.valueobject.StockSnapshot;
import com.wei.orchestrator.wes.domain.port.WesPort;
import com.wei.orchestrator.wes.infrastructure.adapter.dto.WesInventoryDto;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryAdjustmentApplicationService {

    private static final Logger logger =
            LoggerFactory.getLogger(InventoryAdjustmentApplicationService.class);

    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final WesPort wesPort;
    private final InventoryPort inventoryPort;
    private final ApplicationEventPublisher eventPublisher;

    public InventoryAdjustmentApplicationService(
            InventoryAdjustmentRepository inventoryAdjustmentRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            WesPort wesPort,
            InventoryPort inventoryPort,
            ApplicationEventPublisher eventPublisher) {
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.wesPort = wesPort;
        this.inventoryPort = inventoryPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public String detectDiscrepancy(DetectDiscrepancyCommand command) {
        logger.info("Detecting inventory discrepancies for observer: {}", command.getObserverId());

        try {
            List<StockSnapshot> wesSnapshots = getWesStockSnapshots();

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(
                            command.getInventorySnapshots(), wesSnapshots);

            inventoryAdjustmentRepository.save(adjustment);

            if (adjustment.hasDiscrepancies()) {
                publishEvents(adjustment);
                logger.info(
                        "Detected {} discrepancies in adjustment {}",
                        adjustment.getDiscrepancyLogs().size(),
                        adjustment.getAdjustmentId());
                return adjustment.getAdjustmentId();
            } else {
                logger.info("No discrepancies found for observer: {}", command.getObserverId());
                return null;
            }

        } catch (Exception e) {
            logger.error("Error detecting discrepancies: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to detect discrepancies: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void applyAdjustment(ApplyAdjustmentCommand command) {
        logger.info("Applying adjustment: {}", command.getAdjustmentId());

        InventoryAdjustment adjustment =
                inventoryAdjustmentRepository
                        .findById(command.getAdjustmentId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Adjustment not found: "
                                                        + command.getAdjustmentId()));

        if (!adjustment.hasDiscrepancies()) {
            logger.info(
                    "No discrepancies to apply for adjustment: {}", adjustment.getAdjustmentId());
            return;
        }

        try {
            List<DiscrepancyLog> logs = adjustment.getDiscrepancyLogs();

            var warehouseGroups = new java.util.HashMap<String, List<DiscrepancyLog>>();
            for (DiscrepancyLog log : logs) {
                warehouseGroups
                        .computeIfAbsent(log.getWarehouseId(), k -> new ArrayList<>())
                        .add(log);
            }

            for (var entry : warehouseGroups.entrySet()) {
                String warehouseId = entry.getKey();
                List<DiscrepancyLog> warehouseLogs = entry.getValue();

                List<TransactionLine> lines = new ArrayList<>();
                for (DiscrepancyLog log : warehouseLogs) {
                    lines.add(TransactionLine.of(log.getSku(), log.getDifference()));
                }

                InventoryTransaction transaction =
                        InventoryTransaction.createAdjustmentTransaction(
                                adjustment.getAdjustmentId(),
                                TransactionSource.CYCLE_COUNT_ADJUSTMENT,
                                warehouseId,
                                lines);

                inventoryTransactionRepository.save(transaction);

                try {
                    transaction.markAsProcessing();
                    inventoryTransactionRepository.save(transaction);

                    for (TransactionLine line : lines) {
                        inventoryPort.adjustInventory(
                                line.getSku(),
                                warehouseId,
                                line.getQuantity(),
                                "Inventory discrepancy adjustment: "
                                        + adjustment.getAdjustmentId());
                    }

                    transaction.complete();
                    inventoryTransactionRepository.save(transaction);
                    publishEvents(transaction);

                    adjustment.applyAdjustment(transaction.getTransactionId());

                } catch (Exception e) {
                    transaction.fail(e.getMessage());
                    inventoryTransactionRepository.save(transaction);
                    publishEvents(transaction);
                    throw e;
                }
            }

            adjustment.complete();
            inventoryAdjustmentRepository.save(adjustment);
            publishEvents(adjustment);

            logger.info("Successfully applied adjustment: {}", adjustment.getAdjustmentId());

        } catch (Exception e) {
            adjustment.fail(e.getMessage());
            inventoryAdjustmentRepository.save(adjustment);
            publishEvents(adjustment);
            logger.error(
                    "Failed to apply adjustment {}: {}",
                    adjustment.getAdjustmentId(),
                    e.getMessage(),
                    e);
            throw new RuntimeException("Failed to apply adjustment: " + e.getMessage(), e);
        }
    }

    private List<StockSnapshot> getWesStockSnapshots() {
        try {
            List<WesInventoryDto> wesInventories = wesPort.getInventorySnapshot();
            logger.info("Fetched {} inventory items from WES", wesInventories.size());

            return WesTranslator.toStockSnapshots(wesInventories);

        } catch (Exception e) {
            logger.error("Failed to fetch WES inventory snapshot: {}", e.getMessage(), e);
            throw new RuntimeException(
                    "Failed to fetch WES inventory snapshot: " + e.getMessage(), e);
        }
    }

    private void publishEvents(InventoryAdjustment adjustment) {
        adjustment.getDomainEvents().forEach(eventPublisher::publishEvent);
        adjustment.clearDomainEvents();
    }

    private void publishEvents(InventoryTransaction transaction) {
        transaction.getDomainEvents().forEach(eventPublisher::publishEvent);
        transaction.clearDomainEvents();
    }
}
