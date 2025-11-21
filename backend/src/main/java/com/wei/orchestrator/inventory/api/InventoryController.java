package com.wei.orchestrator.inventory.api;

import com.wei.orchestrator.inventory.api.dto.ReserveInventoryRequest;
import com.wei.orchestrator.inventory.api.dto.ReserveInventoryResponse;
import com.wei.orchestrator.inventory.application.InventoryApplicationService;
import com.wei.orchestrator.inventory.application.command.ReserveInventoryCommand;
import com.wei.orchestrator.inventory.application.dto.InventoryOperationResultDto;
import com.wei.orchestrator.inventory.query.InventoryQueryService;
import com.wei.orchestrator.inventory.query.dto.InventoryTransactionDetailDto;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import jakarta.validation.Valid;
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
}
