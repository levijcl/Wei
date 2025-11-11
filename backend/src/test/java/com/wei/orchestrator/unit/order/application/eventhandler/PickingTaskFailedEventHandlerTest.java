package com.wei.orchestrator.unit.order.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.order.application.eventhandler.PickingTaskFailedEventHandler;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.wes.domain.event.PickingTaskFailedEvent;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskOrigin;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PickingTaskFailedEventHandlerTest {

    @Mock private OrderRepository orderRepository;

    @Mock private PickingTaskRepository pickingTaskRepository;

    @InjectMocks private PickingTaskFailedEventHandler eventHandler;

    @Nested
    class handlePickingTaskFailedTest {

        @Test
        void shouldMarkLineItemsAsFailedWhenOrchestratorSubmitted() {
            String taskId = "PICK-001";
            String wesTaskId = "WES-001";
            String orderId = "ORDER-001";
            String reason = "WES timeout";

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            taskId,
                            wesTaskId,
                            orderId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            reason,
                            LocalDateTime.now());

            Order order = createOrderWithSingleItem(orderId, "SKU-001");
            PickingTask pickingTask = createPickingTask(taskId, orderId, "SKU-001");

            when(pickingTaskRepository.findById(taskId)).thenReturn(Optional.of(pickingTask));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            eventHandler.handlePickingTaskFailed(event);

            verify(pickingTaskRepository, times(1)).findById(taskId);
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderRepository, times(1)).save(order);

            assertTrue(order.getOrderLineItems().get(0).hasCommitmentFailed());
            assertEquals(
                    reason,
                    order.getOrderLineItems().get(0).getCommitmentInfo().getFailureReason());
        }

        @Test
        void shouldMarkMultipleLineItemsWhenMultipleSkusInTask() {
            String taskId = "PICK-002";
            String wesTaskId = "WES-002";
            String orderId = "ORDER-002";
            String reason = "Inventory mismatch";

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            taskId,
                            wesTaskId,
                            orderId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            reason,
                            LocalDateTime.now());

            Order order = createOrderWithMultipleItems(orderId);
            PickingTask pickingTask =
                    createPickingTaskWithMultipleSkus(taskId, orderId, "SKU-001", "SKU-002");

            when(pickingTaskRepository.findById(taskId)).thenReturn(Optional.of(pickingTask));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            eventHandler.handlePickingTaskFailed(event);

            verify(orderRepository, times(1)).save(order);

            assertTrue(order.getOrderLineItems().get(0).hasCommitmentFailed());
            assertTrue(order.getOrderLineItems().get(1).hasCommitmentFailed());
        }

        @Test
        void shouldSkipProcessingWhenOriginIsWesDirect() {
            String taskId = "PICK-003";
            String wesTaskId = "WES-003";
            String orderId = "ORDER-003";
            String reason = "WES error";

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            taskId,
                            wesTaskId,
                            orderId,
                            TaskOrigin.WES_DIRECT,
                            reason,
                            LocalDateTime.now());

            eventHandler.handlePickingTaskFailed(event);

            verify(pickingTaskRepository, never()).findById(any());
            verify(orderRepository, never()).findById(any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        void shouldSkipProcessingWhenOrderIdIsNull() {
            String taskId = "PICK-004";
            String wesTaskId = "WES-004";
            String reason = "WES error";

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            taskId,
                            wesTaskId,
                            null,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            reason,
                            LocalDateTime.now());

            eventHandler.handlePickingTaskFailed(event);

            verify(pickingTaskRepository, never()).findById(any());
            verify(orderRepository, never()).findById(any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenPickingTaskNotFound() {
            String taskId = "PICK-005";
            String wesTaskId = "WES-005";
            String orderId = "ORDER-005";
            String reason = "WES error";

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            taskId,
                            wesTaskId,
                            orderId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            reason,
                            LocalDateTime.now());

            when(pickingTaskRepository.findById(taskId)).thenReturn(Optional.empty());

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handlePickingTaskFailed(event);
                            });

            assertTrue(exception.getMessage().contains("Picking task not found"));
            assertTrue(exception.getMessage().contains(taskId));
            verify(pickingTaskRepository, times(1)).findById(taskId);
            verify(orderRepository, never()).findById(any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenOrderNotFound() {
            String taskId = "PICK-006";
            String wesTaskId = "WES-006";
            String orderId = "ORDER-006";
            String reason = "WES error";

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            taskId,
                            wesTaskId,
                            orderId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            reason,
                            LocalDateTime.now());

            PickingTask pickingTask = createPickingTask(taskId, orderId, "SKU-001");

            when(pickingTaskRepository.findById(taskId)).thenReturn(Optional.of(pickingTask));
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handlePickingTaskFailed(event);
                            });

            assertTrue(exception.getMessage().contains("Order not found"));
            assertTrue(exception.getMessage().contains(orderId));
            verify(pickingTaskRepository, times(1)).findById(taskId);
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderRepository, never()).save(any());
        }

        @Test
        void shouldMarkOnlyMatchingSkusAsFailed() {
            String taskId = "PICK-007";
            String wesTaskId = "WES-007";
            String orderId = "ORDER-007";
            String reason = "Partial failure";

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            taskId,
                            wesTaskId,
                            orderId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            reason,
                            LocalDateTime.now());

            Order order = createOrderWithMultipleItems(orderId);
            PickingTask pickingTask = createPickingTask(taskId, orderId, "SKU-001");

            when(pickingTaskRepository.findById(taskId)).thenReturn(Optional.of(pickingTask));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            eventHandler.handlePickingTaskFailed(event);

            verify(orderRepository, times(1)).save(order);

            assertTrue(order.getOrderLineItems().get(0).hasCommitmentFailed());
            assertFalse(order.getOrderLineItems().get(1).hasCommitmentFailed());
        }

        @Test
        void shouldStoreFailureReasonCorrectly() {
            String taskId = "PICK-008";
            String wesTaskId = "WES-008";
            String orderId = "ORDER-008";
            String reason = "Item damaged in warehouse";

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            taskId,
                            wesTaskId,
                            orderId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            reason,
                            LocalDateTime.now());

            Order order = createOrderWithSingleItem(orderId, "SKU-001");
            PickingTask pickingTask = createPickingTask(taskId, orderId, "SKU-001");

            when(pickingTaskRepository.findById(taskId)).thenReturn(Optional.of(pickingTask));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            eventHandler.handlePickingTaskFailed(event);

            assertEquals(
                    reason,
                    order.getOrderLineItems().get(0).getCommitmentInfo().getFailureReason());
        }
    }

    private Order createOrderWithSingleItem(String orderId, String sku) {
        List<OrderLineItem> lineItems =
                List.of(new OrderLineItem(sku, 10, new BigDecimal("100.00")));
        Order order = new Order(orderId, lineItems);
        order.markReadyForFulfillment();
        String lineItemId = order.getOrderLineItems().get(0).getLineItemId();
        order.reserveLineItem(lineItemId, "TX-001", "EXT-RES-001", "WH-001");
        return order;
    }

    private Order createOrderWithMultipleItems(String orderId) {
        List<OrderLineItem> lineItems =
                List.of(
                        new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")),
                        new OrderLineItem("SKU-002", 5, new BigDecimal("50.00")));
        Order order = new Order(orderId, lineItems);
        order.markReadyForFulfillment();
        String lineItemId1 = order.getOrderLineItems().get(0).getLineItemId();
        String lineItemId2 = order.getOrderLineItems().get(1).getLineItemId();
        order.reserveLineItem(lineItemId1, "TX-001", "EXT-RES-001", "WH-001");
        order.reserveLineItem(lineItemId2, "TX-002", "EXT-RES-002", "WH-001");
        return order;
    }

    private PickingTask createPickingTask(String taskId, String orderId, String sku) {
        List<TaskItem> items = List.of(TaskItem.of(sku, 10, "WH-001"));
        PickingTask task = PickingTask.createForOrder(orderId, items, 5);
        task.setTaskId(taskId);
        return task;
    }

    private PickingTask createPickingTaskWithMultipleSkus(
            String taskId, String orderId, String... skus) {
        List<TaskItem> items = new java.util.ArrayList<>();
        for (String sku : skus) {
            items.add(TaskItem.of(sku, 10, "WH-001"));
        }
        PickingTask task = PickingTask.createForOrder(orderId, items, 5);
        task.setTaskId(taskId);
        return task;
    }
}
