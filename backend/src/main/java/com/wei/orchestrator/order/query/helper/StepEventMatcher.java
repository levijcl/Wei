package com.wei.orchestrator.order.query.helper;

import com.wei.orchestrator.order.query.dto.OrderProcessStatusDto;
import com.wei.orchestrator.shared.infrastructure.persistence.AuditRecordEntity;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public interface StepEventMatcher {

    List<AuditRecordEntity> filterEvents(List<AuditRecordEntity> allRecords);

    OrderProcessStatusDto.ProcessStepDto createStepDto(
            int stepNumber, String stepName, List<AuditRecordEntity> filteredEvents);

    List<String> getEventNames();

    static StepEventMatcher singleEvent(String eventName) {
        return new StepEventMatcher() {
            @Override
            public List<AuditRecordEntity> filterEvents(List<AuditRecordEntity> allRecords) {
                return allRecords.stream().filter(r -> r.getEventName().equals(eventName)).toList();
            }

            @Override
            public OrderProcessStatusDto.ProcessStepDto createStepDto(
                    int stepNumber, String stepName, List<AuditRecordEntity> filteredEvents) {
                if (filteredEvents == null || filteredEvents.isEmpty()) {
                    return new OrderProcessStatusDto.ProcessStepDto(
                            stepNumber, stepName, "PENDING", null);
                }

                AuditRecordEntity lastEvent =
                        filteredEvents.stream()
                                .max(Comparator.comparing(AuditRecordEntity::getEventTimestamp))
                                .get();

                return new OrderProcessStatusDto.ProcessStepDto(
                        stepNumber, stepName, "SUCCESS", lastEvent.getEventTimestamp());
            }

            @Override
            public List<String> getEventNames() {
                return List.of(eventName);
            }
        };
    }

    static StepEventMatcher successOrFailure(String successEvent, String failureEvent) {
        return new StepEventMatcher() {
            @Override
            public List<AuditRecordEntity> filterEvents(List<AuditRecordEntity> allRecords) {
                return allRecords.stream()
                        .filter(
                                r ->
                                        r.getEventName().equals(successEvent)
                                                || r.getEventName().equals(failureEvent))
                        .toList();
            }

            @Override
            public OrderProcessStatusDto.ProcessStepDto createStepDto(
                    int stepNumber, String stepName, List<AuditRecordEntity> filteredEvents) {
                if (filteredEvents == null || filteredEvents.isEmpty()) {
                    return new OrderProcessStatusDto.ProcessStepDto(
                            stepNumber, stepName, "PENDING", null);
                }

                Optional<AuditRecordEntity> lastFailure =
                        filteredEvents.stream()
                                .filter(r -> r.getEventName().equals(failureEvent))
                                .max(Comparator.comparing(AuditRecordEntity::getEventTimestamp));

                Optional<AuditRecordEntity> lastSuccess =
                        filteredEvents.stream()
                                .filter(r -> r.getEventName().equals(successEvent))
                                .max(Comparator.comparing(AuditRecordEntity::getEventTimestamp));

                if (lastFailure.isPresent()) {
                    return new OrderProcessStatusDto.ProcessStepDto(
                            stepNumber, stepName, "FAILED", lastFailure.get().getEventTimestamp());
                }

                if (lastSuccess.isPresent()) {
                    return new OrderProcessStatusDto.ProcessStepDto(
                            stepNumber, stepName, "SUCCESS", lastSuccess.get().getEventTimestamp());
                }

                return new OrderProcessStatusDto.ProcessStepDto(
                        stepNumber, stepName, "PENDING", null);
            }

            @Override
            public List<String> getEventNames() {
                return List.of(successEvent, failureEvent);
            }
        };
    }

    static StepEventMatcher pickingCompleted() {
        return new StepEventMatcher() {
            @Override
            public List<AuditRecordEntity> filterEvents(List<AuditRecordEntity> allRecords) {
                List<String> allEventNames =
                        allRecords.stream().map(AuditRecordEntity::getEventName).toList();

                return allRecords.stream()
                        .filter(
                                r ->
                                        r.getEventName().equals("PickingTaskCompletedEvent")
                                                || (r.getEventName()
                                                                .equals("PickingTaskFailedEvent")
                                                        && allEventNames.contains(
                                                                "PickingTaskCreatedEvent"))
                                                || r.getEventName()
                                                        .equals("PickingTaskCanceledEvent"))
                        .toList();
            }

            @Override
            public OrderProcessStatusDto.ProcessStepDto createStepDto(
                    int stepNumber, String stepName, List<AuditRecordEntity> filteredEvents) {
                if (filteredEvents == null || filteredEvents.isEmpty()) {
                    return new OrderProcessStatusDto.ProcessStepDto(
                            stepNumber, stepName, "PENDING", null);
                }

                Optional<AuditRecordEntity> lastFailure =
                        filteredEvents.stream()
                                .filter(
                                        r ->
                                                r.getEventName().equals("PickingTaskFailedEvent")
                                                        || r.getEventName()
                                                                .equals("PickingTaskCanceledEvent"))
                                .max(Comparator.comparing(AuditRecordEntity::getEventTimestamp));

                Optional<AuditRecordEntity> lastSuccess =
                        filteredEvents.stream()
                                .filter(r -> r.getEventName().equals("PickingTaskCompletedEvent"))
                                .max(Comparator.comparing(AuditRecordEntity::getEventTimestamp));

                if (lastFailure.isPresent()) {
                    return new OrderProcessStatusDto.ProcessStepDto(
                            stepNumber, stepName, "FAILED", lastFailure.get().getEventTimestamp());
                }

                if (lastSuccess.isPresent()) {
                    return new OrderProcessStatusDto.ProcessStepDto(
                            stepNumber, stepName, "SUCCESS", lastSuccess.get().getEventTimestamp());
                }

                return new OrderProcessStatusDto.ProcessStepDto(
                        stepNumber, stepName, "PENDING", null);
            }

            @Override
            public List<String> getEventNames() {
                return List.of(
                        "PickingTaskCompletedEvent",
                        "PickingTaskFailedEvent",
                        "PickingTaskCanceledEvent");
            }
        };
    }

    static StepEventMatcher alwaysPending() {
        return new StepEventMatcher() {
            @Override
            public List<AuditRecordEntity> filterEvents(List<AuditRecordEntity> allRecords) {
                return List.of();
            }

            @Override
            public OrderProcessStatusDto.ProcessStepDto createStepDto(
                    int stepNumber, String stepName, List<AuditRecordEntity> filteredEvents) {
                return new OrderProcessStatusDto.ProcessStepDto(
                        stepNumber, stepName, "PENDING", null);
            }

            @Override
            public List<String> getEventNames() {
                return List.of();
            }
        };
    }
}
