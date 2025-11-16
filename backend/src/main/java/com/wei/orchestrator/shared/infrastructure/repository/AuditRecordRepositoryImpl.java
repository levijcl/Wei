package com.wei.orchestrator.shared.infrastructure.repository;

import com.wei.orchestrator.shared.domain.model.AuditRecord;
import com.wei.orchestrator.shared.domain.repository.AuditRecordRepository;
import com.wei.orchestrator.shared.infrastructure.mapper.AuditRecordMapper;
import com.wei.orchestrator.shared.infrastructure.persistence.AuditRecordEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AuditRecordRepositoryImpl implements AuditRecordRepository {

    private final JpaAuditRecordRepository jpaAuditRecordRepository;

    public AuditRecordRepositoryImpl(JpaAuditRecordRepository jpaAuditRecordRepository) {
        this.jpaAuditRecordRepository = jpaAuditRecordRepository;
    }

    @Override
    public void save(AuditRecord auditRecord) {
        AuditRecordEntity entity = AuditRecordMapper.toEntity(auditRecord);
        jpaAuditRecordRepository.save(entity);
    }

    @Override
    public Optional<AuditRecord> findById(UUID recordId) {
        return jpaAuditRecordRepository
                .findById(recordId.toString())
                .map(AuditRecordMapper::toDomain);
    }

    @Override
    public List<AuditRecord> findByAggregateTypeAndId(String aggregateType, String aggregateId) {
        return jpaAuditRecordRepository
                .findByAggregateTypeAndAggregateId(aggregateType, aggregateId)
                .stream()
                .map(AuditRecordMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditRecord> findByEventTimestampBetween(LocalDateTime start, LocalDateTime end) {
        return jpaAuditRecordRepository.findByEventTimestampBetween(start, end).stream()
                .map(AuditRecordMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditRecord> findByCorrelationId(UUID correlationId) {
        return jpaAuditRecordRepository.findByCorrelationId(correlationId.toString()).stream()
                .map(AuditRecordMapper::toDomain)
                .collect(Collectors.toList());
    }
}
