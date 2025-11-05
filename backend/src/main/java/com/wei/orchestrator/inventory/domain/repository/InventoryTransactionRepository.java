package com.wei.orchestrator.inventory.domain.repository;

import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import java.util.List;
import java.util.Optional;

public interface InventoryTransactionRepository {
    InventoryTransaction save(InventoryTransaction transaction);

    Optional<InventoryTransaction> findById(String transactionId);

    List<InventoryTransaction> findBySourceReferenceId(String sourceReferenceId);
}
