package com.wei.orchestrator.order.query;

import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.infrastructure.persistence.OrderEntity;
import com.wei.orchestrator.order.infrastructure.persistence.OrderLineItemEntity;
import com.wei.orchestrator.order.query.dto.OrderDetailDto;
import com.wei.orchestrator.order.query.dto.OrderLineItemDto;
import com.wei.orchestrator.order.query.dto.OrderSummaryDto;
import com.wei.orchestrator.order.query.infrastructure.OrderQueryRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
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
        if (statuses == null || statuses.isEmpty()) {
            return orderQueryRepository.findAllOrderSummaries(pageable);
        }

        List<String> statusStrings = statuses.stream().map(Enum::name).collect(Collectors.toList());

        return orderQueryRepository.findOrderSummaries(statusStrings, pageable);
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
