package com.wei.orchestrator.inventory.infrastructure.repository;

import com.wei.orchestrator.inventory.infrastructure.persistence.TransactionLineEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaTransactionLineRepository extends JpaRepository<TransactionLineEntity, Long> {
    List<TransactionLineEntity> findByTransactionId(String transactionId);

    void deleteByTransactionId(String transactionId);
}
