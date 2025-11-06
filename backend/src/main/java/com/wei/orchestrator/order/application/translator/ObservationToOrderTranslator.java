package com.wei.orchestrator.order.application.translator;

import com.wei.orchestrator.observation.domain.model.valueobject.ObservationResult;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservedOrderItem;
import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.application.command.CreateOrderCommand.OrderLineItemDto;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ObservationToOrderTranslator {

    public CreateOrderCommand translate(ObservationResult observationResult) {
        if (observationResult == null) {
            throw new IllegalArgumentException("ObservationResult cannot be null");
        }

        List<OrderLineItemDto> items =
                observationResult.getItems().stream()
                        .map(this::translateItem)
                        .collect(Collectors.toList());

        CreateOrderCommand command = new CreateOrderCommand(observationResult.getOrderId(), items);
        command.setScheduledPickupTime(observationResult.getScheduledPickupTime());

        return command;
    }

    private OrderLineItemDto translateItem(ObservedOrderItem observedItem) {
        return new OrderLineItemDto(
                observedItem.getSku(), observedItem.getQuantity(), observedItem.getPrice());
    }
}
