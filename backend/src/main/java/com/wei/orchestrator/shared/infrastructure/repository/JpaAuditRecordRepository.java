package com.wei.orchestrator.shared.infrastructure.repository;

import com.wei.orchestrator.shared.infrastructure.persistence.AuditRecordEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAuditRecordRepository extends JpaRepository<AuditRecordEntity, String> {

    List<AuditRecordEntity> findByAggregateTypeAndAggregateId(
            String aggregateType, String aggregateId);

    List<AuditRecordEntity> findByEventTimestampBetween(LocalDateTime start, LocalDateTime end);

    @Query(
            value =
                    "SELECT * FROM audit_records WHERE JSON_VALUE(event_metadata,"
                            + " '$.correlationId') = :correlationId",
            nativeQuery = true)
    List<AuditRecordEntity> findByCorrelationId(@Param("correlationId") String correlationId);
}
