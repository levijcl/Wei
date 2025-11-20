package com.wei.orchestrator.order.query.helper;

import com.wei.orchestrator.order.query.dto.OrderProcessStatusDto;
import com.wei.orchestrator.shared.infrastructure.persistence.AuditRecordEntity;
import java.util.List;

public enum ProcessStep {
    ORDER_RECEIVED(1, "Order Received", StepEventMatcher.singleEvent("NewOrderObservedEvent")),

    ORDER_SCHEDULED(2, "Order Scheduled", StepEventMatcher.singleEvent("OrderScheduledEvent")),

    FULFILLMENT_STARTED(
            3,
            "Fulfillment Started",
            StepEventMatcher.singleEvent("OrderReadyForFulfillmentEvent")),

    INVENTORY_RESERVED(
            4,
            "Inventory Reserved",
            StepEventMatcher.successOrFailure("InventoryReservedEvent", "ReservationFailedEvent")),

    ORDER_RESERVED(5, "Order Reserved", StepEventMatcher.singleEvent("OrderReservedEvent")),

    PICKING_TASK_CREATED(
            6,
            "Picking Task Created",
            StepEventMatcher.successOrFailure("PickingTaskCreatedEvent", "PickingTaskFailedEvent")),

    PICKING_TASK_PROCESSING(
            7,
            "Picking Task Processing",
            StepEventMatcher.singleEvent("WesTaskStatusUpdatedEvent")),

    PICKING_TASK_SUBMITTED(8, "Picking Task Submitted", StepEventMatcher.pickingCompleted()),

    PICKING_COMPLETED(
            9, "Picking Completed", StepEventMatcher.singleEvent("ReservationConsumedEvent"));

    private final int stepNumber;
    private final String stepName;
    private final StepEventMatcher matcher;

    ProcessStep(int stepNumber, String stepName, StepEventMatcher matcher) {
        this.stepNumber = stepNumber;
        this.stepName = stepName;
        this.matcher = matcher;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public String getStepName() {
        return stepName;
    }

    public List<AuditRecordEntity> filterEvents(List<AuditRecordEntity> allRecords) {
        return matcher.filterEvents(allRecords);
    }

    public OrderProcessStatusDto.ProcessStepDto createStepDto(
            List<AuditRecordEntity> filteredEvents) {
        return matcher.createStepDto(stepNumber, stepName, filteredEvents);
    }

    public List<String> getEventNames() {
        return matcher.getEventNames();
    }
}
