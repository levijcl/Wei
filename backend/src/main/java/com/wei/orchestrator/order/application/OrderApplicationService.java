package com.wei.orchestrator.order.application;

import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderApplicationService {

    private final OrderRepository orderRepository;

    public OrderApplicationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order createOrder(CreateOrderCommand command) {
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
