package com.wei.orchestrator.inventory.infrastructure.repository;

import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionSource;
import com.wei.orchestrator.inventory.domain.repository.InventoryTransactionRepository;
import com.wei.orchestrator.inventory.infrastructure.mapper.InventoryTransactionMapper;
import com.wei.orchestrator.inventory.infrastructure.persistence.InventoryTransactionEntity;
import com.wei.orchestrator.inventory.infrastructure.persistence.TransactionLineEntity;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryTransactionRepositoryImpl implements InventoryTransactionRepository {

    private final JpaInventoryTransactionRepository jpaInventoryTransactionRepository;
    private final JpaTransactionLineRepository jpaTransactionLineRepository;

    public InventoryTransactionRepositoryImpl(
            JpaInventoryTransactionRepository jpaInventoryTransactionRepository,
            JpaTransactionLineRepository jpaTransactionLineRepository) {
        this.jpaInventoryTransactionRepository = jpaInventoryTransactionRepository;
        this.jpaTransactionLineRepository = jpaTransactionLineRepository;
    }

    @Override
    @Transactional
    public InventoryTransaction save(InventoryTransaction transaction) {
        InventoryTransactionEntity entity = InventoryTransactionMapper.toEntity(transaction);
        InventoryTransactionEntity savedEntity = jpaInventoryTransactionRepository.save(entity);

        jpaTransactionLineRepository.deleteByTransactionId(transaction.getTransactionId());

        List<TransactionLineEntity> lineEntities =
                InventoryTransactionMapper.toTransactionLineEntities(
                        transaction.getTransactionId(), transaction.getTransactionLines());
        List<TransactionLineEntity> savedLineEntities =
                jpaTransactionLineRepository.saveAll(lineEntities);

        return InventoryTransactionMapper.toDomain(savedEntity, savedLineEntities);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InventoryTransaction> findById(String transactionId) {
        Optional<InventoryTransactionEntity> entityOpt =
                jpaInventoryTransactionRepository.findById(transactionId);
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        List<TransactionLineEntity> lineEntities =
                jpaTransactionLineRepository.findByTransactionId(transactionId);
        return Optional.of(InventoryTransactionMapper.toDomain(entityOpt.get(), lineEntities));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransaction> findBySourceReferenceId(String sourceReferenceId) {
        List<InventoryTransactionEntity> entities =
                jpaInventoryTransactionRepository.findBySourceReferenceId(sourceReferenceId);
        return entities.stream()
                .map(
                        entity -> {
                            List<TransactionLineEntity> lineEntities =
                                    jpaTransactionLineRepository.findByTransactionId(
                                            entity.getTransactionId());
                            return InventoryTransactionMapper.toDomain(entity, lineEntities);
                        })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InventoryTransaction> findActiveReservationByOrderIdAndSku(
            String orderId, String sku) {
        List<InventoryTransactionEntity> entities =
                jpaInventoryTransactionRepository.findCompletedReservationsByOrderIdAndSku(
                        orderId, sku);

        for (InventoryTransactionEntity entity : entities) {
            boolean isConsumed =
                    hasRelatedTransaction(
                            entity.getTransactionId(), TransactionSource.RESERVATION_CONSUMED);
            boolean isReleased = entity.getSource().equals(TransactionSource.RESERVATION_RELEASED);

            if (!isConsumed && !isReleased) {
                List<TransactionLineEntity> lineEntities =
                        jpaTransactionLineRepository.findByTransactionId(entity.getTransactionId());
                return Optional.of(InventoryTransactionMapper.toDomain(entity, lineEntities));
            }
        }

        return Optional.empty();
    }

    private boolean hasRelatedTransaction(String transactionId, TransactionSource source) {
        return jpaInventoryTransactionRepository.existsByRelatedTransactionIdAndSource(
                transactionId, source);
    }
}
