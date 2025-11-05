package com.wei.orchestrator.inventory.infrastructure.repository;

import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionStatus;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionType;
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
    public List<InventoryTransaction> findByStatus(TransactionStatus status) {
        List<InventoryTransactionEntity> entities =
                jpaInventoryTransactionRepository.findByStatus(status);
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
    public List<InventoryTransaction> findByType(TransactionType type) {
        List<InventoryTransactionEntity> entities =
                jpaInventoryTransactionRepository.findByType(type);
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
    public List<InventoryTransaction> findByWarehouseId(String warehouseId) {
        List<InventoryTransactionEntity> entities =
                jpaInventoryTransactionRepository.findByWarehouseId(warehouseId);
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
    public List<InventoryTransaction> findByExternalReservationId(String externalReservationId) {
        List<InventoryTransactionEntity> entities =
                jpaInventoryTransactionRepository.findByExternalReservationId(
                        externalReservationId);
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
    @Transactional
    public void deleteById(String transactionId) {
        jpaTransactionLineRepository.deleteByTransactionId(transactionId);
        jpaInventoryTransactionRepository.deleteById(transactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String transactionId) {
        return jpaInventoryTransactionRepository.existsById(transactionId);
    }
}
