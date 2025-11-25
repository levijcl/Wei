package com.wei.orchestrator.inventory.api;

import com.wei.orchestrator.inventory.api.dto.ReserveInventoryRequest;
import com.wei.orchestrator.inventory.api.dto.ReserveInventoryResponse;
import com.wei.orchestrator.inventory.application.InventoryApplicationService;
import com.wei.orchestrator.inventory.application.command.ReserveInventoryCommand;
import com.wei.orchestrator.inventory.application.dto.InventoryOperationResultDto;
import com.wei.orchestrator.inventory.domain.model.valueobject.AdjustmentStatus;
import com.wei.orchestrator.inventory.query.InventoryQueryService;
import com.wei.orchestrator.inventory.query.dto.DiscrepancyLogDto;
import com.wei.orchestrator.inventory.query.dto.InventoryAdjustmentSummaryDto;
import com.wei.orchestrator.inventory.query.dto.InventoryTransactionDetailDto;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryApplicationService inventoryApplicationService;
    private final InventoryQueryService inventoryQueryService;

    public InventoryController(
            InventoryApplicationService inventoryApplicationService,
            InventoryQueryService inventoryQueryService) {
        this.inventoryApplicationService = inventoryApplicationService;
        this.inventoryQueryService = inventoryQueryService;
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<InventoryTransactionDetailDto> getTransaction(
            @PathVariable String transactionId) {
        InventoryTransactionDetailDto transaction =
                inventoryQueryService.getTransactionById(transactionId);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/reserve")
    public ResponseEntity<ReserveInventoryResponse> reserveInventory(
            @Valid @RequestBody ReserveInventoryRequest request) {

        ReserveInventoryCommand command =
                new ReserveInventoryCommand(
                        request.getOrderId(), request.getSku(), "WH001", request.getQuantity());

        InventoryOperationResultDto result =
                inventoryApplicationService.reserveInventory(command, TriggerContext.manual());

        ReserveInventoryResponse response =
                new ReserveInventoryResponse(
                        result.isSuccess(), result.getTransactionId(), result.getErrorMessage());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/adjustments")
    public ResponseEntity<Page<InventoryAdjustmentSummaryDto>> getAdjustments(
            @RequestParam(required = false) String[] status,
            @PageableDefault(size = 20) Pageable pageable) {

        List<AdjustmentStatus> statuses = null;
        if (status != null && status.length > 0) {
            statuses =
                    Arrays.stream(status)
                            .map(AdjustmentStatus::valueOf)
                            .collect(Collectors.toList());
        }

        Page<InventoryAdjustmentSummaryDto> adjustments =
                inventoryQueryService.getAdjustments(statuses, pageable);
        return ResponseEntity.ok(adjustments);
    }

    @GetMapping("/adjustments/{adjustmentId}/discrepancies")
    public ResponseEntity<List<DiscrepancyLogDto>> getDiscrepancyLogs(
            @PathVariable String adjustmentId) {
        List<DiscrepancyLogDto> logs =
                inventoryQueryService.getDiscrepancyLogsByAdjustmentId(adjustmentId);
        return ResponseEntity.ok(logs);
    }
}
