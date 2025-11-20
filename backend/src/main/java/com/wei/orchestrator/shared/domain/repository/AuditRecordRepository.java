package com.wei.orchestrator.shared.domain.repository;

import com.wei.orchestrator.shared.domain.model.AuditRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditRecordRepository {

    void save(AuditRecord auditRecord);

    Optional<AuditRecord> findById(UUID recordId);

    List<AuditRecord> findByAggregateTypeAndId(String aggregateType, String aggregateId);

    List<AuditRecord> findByEventTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<AuditRecord> findByCorrelationId(UUID correlationId);
}
