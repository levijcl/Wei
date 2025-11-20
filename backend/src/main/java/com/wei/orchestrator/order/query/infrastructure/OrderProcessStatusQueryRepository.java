package com.wei.orchestrator.order.query.infrastructure;

import com.wei.orchestrator.shared.infrastructure.persistence.AuditRecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderProcessStatusQueryRepository
        extends JpaRepository<AuditRecordEntity, String> {

    @Query(
            value =
                    "SELECT * FROM audit_records WHERE JSON_VALUE(payload, '$.orderId') = :orderId"
                            + " ORDER BY event_timestamp ASC",
            nativeQuery = true)
    List<AuditRecordEntity> findByOrderIdInPayload(@Param("orderId") String orderId);
}
