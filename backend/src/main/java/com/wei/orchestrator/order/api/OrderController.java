package com.wei.orchestrator.order.api;

import com.wei.orchestrator.order.api.dto.CreateOrderRequest;
import com.wei.orchestrator.order.api.dto.OrderResponse;
import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.domain.exception.InvalidOrderStatusException;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.query.OrderQueryService;
import com.wei.orchestrator.order.query.dto.OrderDetailDto;
import com.wei.orchestrator.order.query.dto.OrderSummaryDto;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderApplicationService orderApplicationService;
    private final OrderQueryService orderQueryService;

    public OrderController(
            OrderApplicationService orderApplicationService, OrderQueryService orderQueryService) {
        this.orderApplicationService = orderApplicationService;
        this.orderQueryService = orderQueryService;
    }

    @GetMapping
    public ResponseEntity<Page<OrderSummaryDto>> getOrders(
            @RequestParam(required = false) String[] status,
            @PageableDefault(size = 20) Pageable pageable) {

        List<OrderStatus> statuses = null;
        try {
            statuses = Arrays.stream(status).map(OrderStatus::valueOf).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            String invalidValue =
                    e.getMessage().contains("No enum constant")
                            ? e.getMessage().substring(e.getMessage().lastIndexOf(".") + 1)
                            : Arrays.toString(status);
            throw new InvalidOrderStatusException(invalidValue);
        }

        Page<OrderSummaryDto> orders = orderQueryService.getOrders(statuses, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailDto> getOrderDetail(@PathVariable String orderId) {
        OrderDetailDto orderDetail = orderQueryService.getOrderDetail(orderId);
        return ResponseEntity.ok(orderDetail);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        CreateOrderCommand command =
                new CreateOrderCommand(
                        request.getOrderId(),
                        request.getItems().stream()
                                .map(
                                        item ->
                                                new CreateOrderCommand.OrderLineItemDto(
                                                        item.getSku(),
                                                        item.getQuantity(),
                                                        item.getPrice()))
                                .collect(Collectors.toList()));

        Order order = orderApplicationService.createOrder(command, TriggerContext.manual());

        OrderResponse response = toOrderResponse(order);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private OrderResponse toOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setStatus(order.getStatus().name());

        response.setItems(
                order.getOrderLineItems().stream()
                        .map(this::toOrderLineItemDto)
                        .collect(Collectors.toList()));

        if (order.getShipmentInfo() != null) {
            response.setShipmentInfo(
                    new OrderResponse.ShipmentInfoDto(
                            order.getShipmentInfo().getCarrier(),
                            order.getShipmentInfo().getTrackingNumber()));
        }

        return response;
    }

    private OrderResponse.OrderLineItemDto toOrderLineItemDto(OrderLineItem item) {
        return new OrderResponse.OrderLineItemDto(
                item.getSku(), item.getQuantity(), item.getPrice());
    }
}
