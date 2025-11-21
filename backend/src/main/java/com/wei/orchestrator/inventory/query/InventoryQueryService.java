package com.wei.orchestrator.inventory.query;

import com.wei.orchestrator.inventory.query.dto.InventoryTransactionDetailDto;

public interface InventoryQueryService {

    InventoryTransactionDetailDto getTransactionById(String transactionId);
}
