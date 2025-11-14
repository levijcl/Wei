package com.wei.orchestrator.inventory.application.eventhandler;

import com.wei.orchestrator.inventory.application.InventoryAdjustmentApplicationService;
import com.wei.orchestrator.inventory.application.command.DetectDiscrepancyCommand;
import com.wei.orchestrator.observation.domain.event.InventorySnapshotObservedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component("InventorySnapshotObservedEventHandler")
public class InventorySnapshotObservedEventHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(InventorySnapshotObservedEventHandler.class);

    private final InventoryAdjustmentApplicationService inventoryAdjustmentApplicationService;

    public InventorySnapshotObservedEventHandler(
            InventoryAdjustmentApplicationService inventoryAdjustmentApplicationService) {
        this.inventoryAdjustmentApplicationService = inventoryAdjustmentApplicationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleInventorySnapshotObserved(InventorySnapshotObservedEvent event) {
        logger.info(
                "Handling inventory snapshot observed event for observer: {}",
                event.getObserverId());

        if (event.getSnapshots() == null || event.getSnapshots().isEmpty()) {
            logger.warn(
                    "Inventory snapshot event from observer {} has no snapshots, skipping"
                            + " discrepancy detection",
                    event.getObserverId());
            return;
        }

        try {
            DetectDiscrepancyCommand command =
                    new DetectDiscrepancyCommand(event.getObserverId(), event.getSnapshots());

            String adjustmentId = inventoryAdjustmentApplicationService.detectDiscrepancy(command);

            if (adjustmentId != null) {
                logger.info(
                        "Created adjustment {} for observer: {} with {} snapshots",
                        adjustmentId,
                        event.getObserverId(),
                        event.getSnapshots().size());
            } else {
                logger.info(
                        "No discrepancies detected for observer: {} with {} snapshots",
                        event.getObserverId(),
                        event.getSnapshots().size());
            }
        } catch (Exception e) {
            logger.error(
                    "Failed to detect discrepancies for observer: {}, error: {}",
                    event.getObserverId(),
                    e.getMessage(),
                    e);
        }
    }
}
