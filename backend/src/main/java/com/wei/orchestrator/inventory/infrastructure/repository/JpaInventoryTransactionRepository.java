package com.wei.orchestrator.inventory.infrastructure.repository;

import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionStatus;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionType;
import com.wei.orchestrator.inventory.infrastructure.persistence.InventoryTransactionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaInventoryTransactionRepository
        extends JpaRepository<InventoryTransactionEntity, String> {
    List<InventoryTransactionEntity> findBySourceReferenceId(String sourceReferenceId);

    List<InventoryTransactionEntity> findByStatus(TransactionStatus status);

    List<InventoryTransactionEntity> findByType(TransactionType type);

    List<InventoryTransactionEntity> findByWarehouseId(String warehouseId);

    List<InventoryTransactionEntity> findByExternalReservationId(String externalReservationId);
}
