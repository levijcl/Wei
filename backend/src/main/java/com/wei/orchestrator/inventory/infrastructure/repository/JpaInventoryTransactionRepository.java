package com.wei.orchestrator.inventory.infrastructure.repository;

import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionSource;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionStatus;
import com.wei.orchestrator.inventory.infrastructure.persistence.InventoryTransactionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaInventoryTransactionRepository
        extends JpaRepository<InventoryTransactionEntity, String> {
    List<InventoryTransactionEntity> findBySourceReferenceId(String sourceReferenceId);

    List<InventoryTransactionEntity> findByStatus(TransactionStatus status);

    @Query(
            "SELECT t FROM InventoryTransactionEntity t "
                    + "JOIN t.transactionLines l "
                    + "WHERE t.sourceReferenceId = :orderId "
                    + "AND l.sku = :sku "
                    + "AND t.status = 'COMPLETED' "
                    + "AND t.source = 'ORDER_RESERVATION' "
                    + "ORDER BY t.createdAt DESC")
    List<InventoryTransactionEntity> findCompletedReservationsByOrderIdAndSku(
            @Param("orderId") String orderId, @Param("sku") String sku);

    boolean existsByRelatedTransactionIdAndSource(
            String relatedTransactionId, TransactionSource source);
}
