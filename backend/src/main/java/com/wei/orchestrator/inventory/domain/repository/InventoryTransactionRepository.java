package com.wei.orchestrator.inventory.domain.repository;

import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionStatus;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionType;
import java.util.List;
import java.util.Optional;

public interface InventoryTransactionRepository {
    InventoryTransaction save(InventoryTransaction transaction);

    Optional<InventoryTransaction> findById(String transactionId);

    List<InventoryTransaction> findBySourceReferenceId(String sourceReferenceId);

    List<InventoryTransaction> findByStatus(TransactionStatus status);

    List<InventoryTransaction> findByType(TransactionType type);

    List<InventoryTransaction> findByWarehouseId(String warehouseId);

    List<InventoryTransaction> findByExternalReservationId(String externalReservationId);

    void deleteById(String transactionId);

    boolean existsById(String transactionId);
}
