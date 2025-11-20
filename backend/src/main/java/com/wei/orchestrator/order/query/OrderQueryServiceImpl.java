package com.wei.orchestrator.order.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.infrastructure.persistence.OrderEntity;
import com.wei.orchestrator.order.infrastructure.persistence.OrderLineItemEntity;
import com.wei.orchestrator.order.query.dto.OrderDetailDto;
import com.wei.orchestrator.order.query.dto.OrderLineItemDto;
import com.wei.orchestrator.order.query.dto.OrderProcessStatusDto;
import com.wei.orchestrator.order.query.dto.OrderSummaryDto;
import com.wei.orchestrator.order.query.dto.ProcessStepDetailDto;
import com.wei.orchestrator.order.query.helper.ProcessStep;
import com.wei.orchestrator.order.query.infrastructure.OrderProcessStatusQueryRepository;
import com.wei.orchestrator.order.query.infrastructure.OrderQueryRepository;
import com.wei.orchestrator.shared.infrastructure.persistence.AuditRecordEntity;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.query.PickingTaskQueryService;
import com.wei.orchestrator.wes.query.dto.PickingTaskSummaryDto;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderQueryServiceImpl implements OrderQueryService {

    private final OrderQueryRepository orderQueryRepository;
    private final PickingTaskQueryService pickingTaskQueryService;
    private final OrderProcessStatusQueryRepository processStatusQueryRepository;
    private final ObjectMapper objectMapper;

    public OrderQueryServiceImpl(
            PickingTaskQueryService pickingTaskQueryService,
            OrderQueryRepository orderQueryRepository,
            OrderProcessStatusQueryRepository processStatusQueryRepository) {
        this.orderQueryRepository = orderQueryRepository;
        this.pickingTaskQueryService = pickingTaskQueryService;
        this.processStatusQueryRepository = processStatusQueryRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public Page<OrderSummaryDto> getOrders(List<OrderStatus> statuses, Pageable pageable) {
        Page<Object[]> resultPage;

        if (statuses == null || statuses.isEmpty()) {
            resultPage = orderQueryRepository.findAllOrderSummariesNative(pageable);
        } else {
            List<String> statusStrings =
                    statuses.stream().map(Enum::name).collect(Collectors.toList());
            resultPage = orderQueryRepository.findOrderSummariesNative(statusStrings, pageable);
        }

        List<String> orderIds =
                resultPage.getContent().stream()
                        .map(
                                o -> {
                                    return (String) o[0];
                                })
                        .toList();
        Map<String, List<PickingTaskSummaryDto>> pickingTaskByOrderId =
                getPickingTaskByOrderIds(orderIds);
        List<OrderSummaryDto> dtos =
                resultPage.getContent().stream()
                        .map(
                                (content) -> {
                                    OrderSummaryDto summary = mapToSummaryDto(content);
                                    if (summary.getStatus().equals(OrderStatus.RESERVED.toString())
                                            && isAnyTaskInProgress(
                                                    pickingTaskByOrderId.get(
                                                            summary.getOrderId()))) {
                                        summary.setStatus("IN PROGRESS");
                                    }
                                    return summary;
                                })
                        .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, resultPage.getTotalElements());
    }

    @Override
    public OrderDetailDto getOrderDetail(String orderId) {
        OrderEntity entity =
                orderQueryRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Order not found: " + orderId));

        return mapToDetailDto(entity);
    }

    Map<String, List<PickingTaskSummaryDto>> getPickingTaskByOrderIds(List<String> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        Map<String, List<PickingTaskSummaryDto>> tasksByOrderId = new HashMap<>();
        List<String> filteredOrderIds = orderIds.stream().distinct().toList();
        for (String orderId : filteredOrderIds) {
            List<PickingTaskSummaryDto> tasks =
                    pickingTaskQueryService.getPickingTasksByOrderId(orderId);
            tasksByOrderId.put(orderId, tasks);
        }

        return tasksByOrderId;
    }

    private boolean isAnyTaskInProgress(List<PickingTaskSummaryDto> pickingTasks) {
        if (pickingTasks == null || pickingTasks.isEmpty()) {
            return false;
        }
        return pickingTasks.stream()
                .map(PickingTaskSummaryDto::getStatus)
                .anyMatch(status -> status.equals(TaskStatus.IN_PROGRESS));
    }

    private OrderSummaryDto mapToSummaryDto(Object[] row) {
        String orderId = (String) row[0];
        String status = (String) row[1];
        LocalDateTime scheduledPickupTime =
                row[2] instanceof Timestamp
                        ? ((Timestamp) row[2]).toLocalDateTime()
                        : (LocalDateTime) row[2];
        String carrier = (String) row[3];
        String trackingNumber = (String) row[4];
        Long lineCount = ((Number) row[5]).longValue();
        Long totalQuantity = ((Number) row[6]).longValue();

        return new OrderSummaryDto(
                orderId,
                status,
                scheduledPickupTime,
                carrier,
                trackingNumber,
                lineCount,
                totalQuantity);
    }

    private OrderDetailDto mapToDetailDto(OrderEntity entity) {
        List<OrderLineItemDto> lineItems =
                entity.getOrderLineItems().stream()
                        .map(this::mapToLineItemDto)
                        .collect(Collectors.toList());

        return new OrderDetailDto(
                entity.getOrderId(),
                OrderStatus.valueOf(entity.getStatus()),
                entity.getScheduledPickupTime(),
                entity.getFulfillmentLeadTimeMinutes(),
                entity.getShipmentCarrier(),
                entity.getShipmentTrackingNumber(),
                lineItems);
    }

    private OrderLineItemDto mapToLineItemDto(OrderLineItemEntity entity) {
        OrderLineItemDto.ReservationInfoDto reservationInfo =
                new OrderLineItemDto.ReservationInfoDto(
                        entity.getReservationStatus(),
                        entity.getReservationTransactionId(),
                        entity.getReservationExternalReservationId(),
                        entity.getReservationWarehouseId(),
                        entity.getReservationFailureReason(),
                        entity.getReservationReservedAt());

        OrderLineItemDto.CommitmentInfoDto commitmentInfo =
                new OrderLineItemDto.CommitmentInfoDto(
                        entity.getCommitmentStatus(),
                        entity.getCommitmentWesTransactionId(),
                        entity.getCommitmentFailureReason(),
                        entity.getCommitmentCommittedAt());

        return new OrderLineItemDto(
                entity.getId(),
                entity.getSku(),
                entity.getQuantity(),
                entity.getPrice(),
                reservationInfo,
                commitmentInfo);
    }

    @Override
    public OrderProcessStatusDto getOrderProcessStatus(String orderId) {
        List<AuditRecordEntity> auditRecords =
                processStatusQueryRepository.findByOrderIdInPayload(orderId);

        List<OrderProcessStatusDto.ProcessStepDto> steps = mapToProcessSteps(auditRecords);

        return new OrderProcessStatusDto(orderId, steps);
    }

    private List<OrderProcessStatusDto.ProcessStepDto> mapToProcessSteps(
            List<AuditRecordEntity> auditRecords) {
        return Arrays.stream(ProcessStep.values())
                .map(
                        step -> {
                            List<AuditRecordEntity> stepEvents = step.filterEvents(auditRecords);
                            return step.createStepDto(stepEvents);
                        })
                .collect(Collectors.toList());
    }

    @Override
    public ProcessStepDetailDto getOrderProcessStepDetail(String orderId, int stepNumber) {
        if (stepNumber < 1 || stepNumber > 9) {
            throw new IllegalArgumentException(
                    "Invalid step number: " + stepNumber + ". Must be between 1 and 9.");
        }

        ProcessStep step = ProcessStep.values()[stepNumber - 1];

        List<AuditRecordEntity> allRecords =
                processStatusQueryRepository.findByOrderIdInPayload(orderId);

        List<AuditRecordEntity> stepEvents = step.filterEvents(allRecords);

        OrderProcessStatusDto.ProcessStepDto stepSummary = step.createStepDto(stepEvents);

        List<ProcessStepDetailDto.EventDetailDto> eventDetails =
                stepEvents.stream()
                        .sorted(
                                java.util.Comparator.comparing(
                                        AuditRecordEntity::getEventTimestamp))
                        .map(this::mapToEventDetailDto)
                        .collect(Collectors.toList());

        return new ProcessStepDetailDto(
                stepNumber, step.getStepName(), stepSummary.getStatus(), eventDetails);
    }

    private ProcessStepDetailDto.EventDetailDto mapToEventDetailDto(AuditRecordEntity entity) {
        try {
            Object parsedPayload = objectMapper.readValue(entity.getPayload(), Object.class);

            ProcessStepDetailDto.EventMetadataDto metadata =
                    parseEventMetadata(entity.getEventMetadata());

            return new ProcessStepDetailDto.EventDetailDto(
                    entity.getRecordId(),
                    entity.getEventName(),
                    entity.getEventTimestamp(),
                    metadata,
                    parsedPayload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse audit record: " + entity.getRecordId(), e);
        }
    }

    private ProcessStepDetailDto.EventMetadataDto parseEventMetadata(String metadataJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadataMap = objectMapper.readValue(metadataJson, Map.class);

            return new ProcessStepDetailDto.EventMetadataDto(
                    (String) metadataMap.get("context"),
                    (String) metadataMap.get("correlationId"),
                    (String) metadataMap.get("triggerSource"),
                    (String) metadataMap.get("triggerBy"));
        } catch (JsonProcessingException e) {
            return new ProcessStepDetailDto.EventMetadataDto(null, null, null, null);
        }
    }
}
