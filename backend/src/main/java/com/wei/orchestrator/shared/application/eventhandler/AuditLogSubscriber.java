package com.wei.orchestrator.shared.application.eventhandler;

import com.wei.orchestrator.shared.application.factory.AuditRecordFactory;
import com.wei.orchestrator.shared.domain.event.DomainEvent;
import com.wei.orchestrator.shared.domain.model.AuditRecord;
import com.wei.orchestrator.shared.domain.repository.AuditRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AuditLogSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogSubscriber.class);

    private final AuditRecordFactory auditRecordFactory;
    private final AuditRecordRepository auditRecordRepository;

    public AuditLogSubscriber(
            AuditRecordFactory auditRecordFactory, AuditRecordRepository auditRecordRepository) {
        this.auditRecordFactory = auditRecordFactory;
        this.auditRecordRepository = auditRecordRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDomainEvent(DomainEvent event) {
        try {
            AuditRecord auditRecord = auditRecordFactory.createAuditRecord(event);
            auditRecordRepository.save(auditRecord);

            logger.info(
                    "Audit record created: recordId={}, event={}, aggregate={}/{}",
                    auditRecord.getRecordId(),
                    auditRecord.getEventName(),
                    auditRecord.getAggregateType(),
                    auditRecord.getAggregateId());

        } catch (Exception e) {
            logger.error(
                    "Failed to record audit for event: {}", event.getClass().getSimpleName(), e);
        }
    }
}
