package com.wei.orchestrator.observation.infrastructure.repository;

import com.wei.orchestrator.observation.domain.model.InventoryObserver;
import com.wei.orchestrator.observation.domain.repository.InventoryObserverRepository;
import com.wei.orchestrator.observation.infrastructure.mapper.InventoryObserverMapper;
import com.wei.orchestrator.observation.infrastructure.persistence.InventoryObserverEntity;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryObserverRepositoryImpl implements InventoryObserverRepository {

    private final JpaInventoryObserverRepository jpaRepository;

    public InventoryObserverRepositoryImpl(JpaInventoryObserverRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public InventoryObserver save(InventoryObserver inventoryObserver) {
        InventoryObserverEntity entity = InventoryObserverMapper.toEntity(inventoryObserver);
        InventoryObserverEntity savedEntity = jpaRepository.save(entity);
        return InventoryObserverMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<InventoryObserver> findById(String observerId) {
        return jpaRepository.findById(observerId).map(InventoryObserverMapper::toDomain);
    }

    @Override
    public List<InventoryObserver> findAllActive() {
        return jpaRepository.findByActiveTrue().stream()
                .map(InventoryObserverMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String observerId) {
        jpaRepository.deleteById(observerId);
    }
}
