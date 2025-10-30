package com.wei.orchestrator.order.application;

import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.order.domain.service.OrderDomainService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final OrderDomainService orderDomainService;

    public OrderApplicationService(
            OrderRepository orderRepository, OrderDomainService orderDomainService) {
        this.orderRepository = orderRepository;
        this.orderDomainService = orderDomainService;
    }

    public Order createOrder(CreateOrderCommand command) {
        orderDomainService.validateOrderCreation(command.getOrderId());

        List<OrderLineItem> lineItems =
                command.getItems().stream()
                        .map(
                                dto ->
                                        new OrderLineItem(
                                                dto.getSku(), dto.getQuantity(), dto.getPrice()))
                        .collect(Collectors.toList());

        Order order = new Order(command.getOrderId(), lineItems);
        order.createOrder();

        return orderRepository.save(order);
    }
}
