package com.wei.orchestrator.inventory.query;

import com.wei.orchestrator.inventory.domain.model.valueobject.AdjustmentStatus;
import com.wei.orchestrator.inventory.query.dto.DiscrepancyLogDto;
import com.wei.orchestrator.inventory.query.dto.InventoryAdjustmentSummaryDto;
import com.wei.orchestrator.inventory.query.dto.InventoryTransactionDetailDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryQueryService {

    InventoryTransactionDetailDto getTransactionById(String transactionId);

    Page<InventoryAdjustmentSummaryDto> getAdjustments(
            List<AdjustmentStatus> statuses, Pageable pageable);

    List<DiscrepancyLogDto> getDiscrepancyLogsByAdjustmentId(String adjustmentId);
}
