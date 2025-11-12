package com.wei.orchestrator.observation.infrastructure.repository;

import com.wei.orchestrator.observation.domain.model.WesObserver;
import com.wei.orchestrator.observation.domain.repository.WesObserverRepository;
import com.wei.orchestrator.observation.infrastructure.mapper.WesObserverMapper;
import com.wei.orchestrator.observation.infrastructure.persistence.WesObserverEntity;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class WesObserverRepositoryImpl implements WesObserverRepository {

    private final JpaWesObserverRepository jpaRepository;

    public WesObserverRepositoryImpl(JpaWesObserverRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public WesObserver save(WesObserver wesObserver) {
        WesObserverEntity entity = WesObserverMapper.toEntity(wesObserver);
        WesObserverEntity savedEntity = jpaRepository.save(entity);
        return WesObserverMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<WesObserver> findById(String observerId) {
        return jpaRepository.findById(observerId).map(WesObserverMapper::toDomain);
    }

    @Override
    public List<WesObserver> findAllActive() {
        return jpaRepository.findByActiveTrue().stream()
                .map(WesObserverMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String observerId) {
        jpaRepository.deleteById(observerId);
    }
}
