package com.wei.orchestrator.order.query;

import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.infrastructure.persistence.OrderEntity;
import com.wei.orchestrator.order.infrastructure.persistence.OrderLineItemEntity;
import com.wei.orchestrator.order.query.dto.OrderDetailDto;
import com.wei.orchestrator.order.query.dto.OrderLineItemDto;
import com.wei.orchestrator.order.query.dto.OrderSummaryDto;
import com.wei.orchestrator.order.query.infrastructure.OrderQueryRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
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

    public OrderQueryServiceImpl(OrderQueryRepository orderQueryRepository) {
        this.orderQueryRepository = orderQueryRepository;
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

        List<OrderSummaryDto> dtos =
                resultPage.getContent().stream()
                        .map(this::mapToSummaryDto)
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
}
