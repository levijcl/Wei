package com.wei.orchestrator.inventory.query.infrastructure;

import com.wei.orchestrator.inventory.infrastructure.persistence.InventoryTransactionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryTransactionQueryRepository
        extends JpaRepository<InventoryTransactionEntity, String> {

    @EntityGraph(attributePaths = {"transactionLines"})
    @Override
    Optional<InventoryTransactionEntity> findById(String id);
}
