package com.wei.orchestrator.observation.infrastructure.repository;

import com.wei.orchestrator.observation.domain.model.OrderObserver;
import com.wei.orchestrator.observation.domain.repository.OrderObserverRepository;
import com.wei.orchestrator.observation.infrastructure.mapper.OrderObserverMapper;
import com.wei.orchestrator.observation.infrastructure.persistence.OrderObserverEntity;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class OrderObserverRepositoryImpl implements OrderObserverRepository {

    private final JpaOrderObserverRepository jpaRepository;
    private final OrderObserverMapper mapper;

    public OrderObserverRepositoryImpl(
            JpaOrderObserverRepository jpaRepository, OrderObserverMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public OrderObserver save(OrderObserver orderObserver) {
        OrderObserverEntity entity = mapper.toEntity(orderObserver);
        OrderObserverEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<OrderObserver> findById(String observerId) {
        return jpaRepository.findById(observerId).map(mapper::toDomain);
    }

    @Override
    public List<OrderObserver> findAllActive() {
        return jpaRepository.findByActiveTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String observerId) {
        jpaRepository.deleteById(observerId);
    }
}
