package com.wei.orchestrator.order.api;

import com.wei.orchestrator.order.api.dto.CreateOrderRequest;
import com.wei.orchestrator.order.api.dto.OrderResponse;
import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import jakarta.validation.Valid;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    public OrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
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

        Order order = orderApplicationService.createOrder(command);

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
