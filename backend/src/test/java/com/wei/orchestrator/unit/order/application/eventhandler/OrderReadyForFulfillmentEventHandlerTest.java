package com.wei.orchestrator.unit.order.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.inventory.application.InventoryApplicationService;
import com.wei.orchestrator.inventory.application.command.ReserveInventoryCommand;
import com.wei.orchestrator.inventory.application.dto.InventoryOperationResultDto;
import com.wei.orchestrator.order.application.eventhandler.OrderReadyForFulfillmentEventHandler;
import com.wei.orchestrator.order.domain.event.OrderReadyForFulfillmentEvent;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderReadyForFulfillmentEventHandlerTest {

    @Mock private OrderRepository orderRepository;

    @Mock private InventoryApplicationService inventoryApplicationService;

    @InjectMocks private OrderReadyForFulfillmentEventHandler eventHandler;

    @Nested
    class handleOrderReadyForFulfillmentTest {

        @Test
        void shouldReserveInventoryForAllLineItems() {
            String orderId = "ORDER-001";
            OrderReadyForFulfillmentEvent event = new OrderReadyForFulfillmentEvent(orderId);
            Order order = createMockOrder(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(inventoryApplicationService.reserveInventory(any()))
                    .thenReturn(InventoryOperationResultDto.success("reservation-1"));

            eventHandler.handleOrderReadyForFulfillment(event);

            verify(orderRepository, times(1)).findById(orderId);
            verify(inventoryApplicationService, times(2)).reserveInventory(any());
        }

        @Test
        void shouldReserveInventoryWithCorrectParameters() {
            String orderId = "ORDER-002";
            OrderReadyForFulfillmentEvent event = new OrderReadyForFulfillmentEvent(orderId);
            Order order = createMockOrder(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(inventoryApplicationService.reserveInventory(any()))
                    .thenReturn(InventoryOperationResultDto.success("reservation-1"));

            ArgumentCaptor<ReserveInventoryCommand> commandCaptor =
                    ArgumentCaptor.forClass(ReserveInventoryCommand.class);

            eventHandler.handleOrderReadyForFulfillment(event);

            verify(inventoryApplicationService, times(2)).reserveInventory(commandCaptor.capture());

            List<ReserveInventoryCommand> commands = commandCaptor.getAllValues();
            assertEquals(2, commands.size());

            assertEquals(orderId, commands.get(0).getOrderId());
            assertEquals("SKU-001", commands.get(0).getSku());
            assertEquals("DEFAULT_WAREHOUSE", commands.get(0).getWarehouseId());
            assertEquals(10, commands.get(0).getQuantity());

            assertEquals(orderId, commands.get(1).getOrderId());
            assertEquals("SKU-002", commands.get(1).getSku());
            assertEquals("DEFAULT_WAREHOUSE", commands.get(1).getWarehouseId());
            assertEquals(5, commands.get(1).getQuantity());
        }

        @Test
        void shouldThrowExceptionWhenOrderNotFound() {
            String orderId = "ORDER-003";
            OrderReadyForFulfillmentEvent event = new OrderReadyForFulfillmentEvent(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handleOrderReadyForFulfillment(event);
                            });

            assertTrue(exception.getMessage().contains("Order not found"));
            assertTrue(exception.getMessage().contains(orderId));
            verify(orderRepository, times(1)).findById(orderId);
            verify(inventoryApplicationService, never()).reserveInventory(any());
        }

        @Test
        void shouldPropagateExceptionWhenInventoryReservationFails() {
            String orderId = "ORDER-004";
            OrderReadyForFulfillmentEvent event = new OrderReadyForFulfillmentEvent(orderId);
            Order order = createMockOrder(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            doThrow(new RuntimeException("Inventory service unavailable"))
                    .when(inventoryApplicationService)
                    .reserveInventory(any());

            RuntimeException exception =
                    assertThrows(
                            RuntimeException.class,
                            () -> {
                                eventHandler.handleOrderReadyForFulfillment(event);
                            });

            assertTrue(exception.getMessage().contains("Inventory service unavailable"));
            verify(orderRepository, times(1)).findById(orderId);
            verify(inventoryApplicationService, times(1)).reserveInventory(any());
        }

        @Test
        void shouldProcessSingleLineItem() {
            String orderId = "ORDER-005";
            OrderReadyForFulfillmentEvent event = new OrderReadyForFulfillmentEvent(orderId);
            Order order = createSingleItemOrder(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(inventoryApplicationService.reserveInventory(any()))
                    .thenReturn(InventoryOperationResultDto.success("reservation-1"));

            eventHandler.handleOrderReadyForFulfillment(event);

            verify(orderRepository, times(1)).findById(orderId);
            verify(inventoryApplicationService, times(1)).reserveInventory(any());
        }

        @Test
        void shouldProcessMultipleLineItems() {
            String orderId = "ORDER-006";
            OrderReadyForFulfillmentEvent event = new OrderReadyForFulfillmentEvent(orderId);
            Order order = createMultiItemOrder(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(inventoryApplicationService.reserveInventory(any()))
                    .thenReturn(InventoryOperationResultDto.success("reservation-1"));

            eventHandler.handleOrderReadyForFulfillment(event);

            verify(orderRepository, times(1)).findById(orderId);
            verify(inventoryApplicationService, times(4)).reserveInventory(any());
        }
    }

    private Order createMockOrder(String orderId) {
        List<OrderLineItem> lineItems =
                Arrays.asList(
                        new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")),
                        new OrderLineItem("SKU-002", 5, new BigDecimal("50.00")));

        return new Order(orderId, lineItems);
    }

    private Order createSingleItemOrder(String orderId) {
        List<OrderLineItem> lineItems =
                List.of(new OrderLineItem("SKU-100", 3, new BigDecimal("30.00")));

        return new Order(orderId, lineItems);
    }

    private Order createMultiItemOrder(String orderId) {
        List<OrderLineItem> lineItems =
                Arrays.asList(
                        new OrderLineItem("SKU-200", 1, new BigDecimal("10.00")),
                        new OrderLineItem("SKU-201", 2, new BigDecimal("20.00")),
                        new OrderLineItem("SKU-202", 3, new BigDecimal("30.00")),
                        new OrderLineItem("SKU-203", 4, new BigDecimal("40.00")));

        return new Order(orderId, lineItems);
    }
}
