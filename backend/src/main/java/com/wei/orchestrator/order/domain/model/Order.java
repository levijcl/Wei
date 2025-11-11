package com.wei.orchestrator.order.domain.model;

import com.wei.orchestrator.order.domain.event.OrderReadyForFulfillmentEvent;
import com.wei.orchestrator.order.domain.event.OrderReservedEvent;
import com.wei.orchestrator.order.domain.event.OrderScheduledEvent;
import com.wei.orchestrator.order.domain.model.valueobject.FulfillmentLeadTime;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.model.valueobject.ScheduledPickupTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {
    private String orderId;
    private OrderStatus status;
    private ScheduledPickupTime scheduledPickupTime;
    private FulfillmentLeadTime fulfillmentLeadTime;
    private ShipmentInfo shipmentInfo;
    private List<OrderLineItem> orderLineItems;
    private final List<Object> domainEvents;

    public Order() {
        this.orderLineItems = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
    }

    public Order(String orderId, List<OrderLineItem> orderLineItems) {
        if (orderLineItems == null || orderLineItems.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one line item");
        }
        this.orderId = orderId;
        this.status = OrderStatus.CREATED;
        this.orderLineItems = new ArrayList<>(orderLineItems);
        this.domainEvents = new ArrayList<>();
    }

    public void createOrder() {
        if (this.orderLineItems == null || this.orderLineItems.isEmpty()) {
            throw new IllegalStateException("Order must have at least one line item");
        }
        this.status = OrderStatus.CREATED;
    }

    public void commitOrder() {
        if (this.status != OrderStatus.RESERVED) {
            throw new IllegalStateException("Cannot commit order in status: " + this.status);
        }
        this.status = OrderStatus.COMMITTED;
    }

    public void markAsShipped(ShipmentInfo shipmentInfo) {
        if (this.status != OrderStatus.COMMITTED) {
            throw new IllegalStateException(
                    "Cannot mark order as shipped in status: " + this.status);
        }
        this.shipmentInfo = shipmentInfo;
        this.status = OrderStatus.SHIPPED;
    }

    public void scheduleForLaterFulfillment(
            ScheduledPickupTime scheduledPickupTime, FulfillmentLeadTime fulfillmentLeadTime) {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Cannot schedule order for later fulfillment in status: " + this.status);
        }
        if (scheduledPickupTime == null) {
            throw new IllegalArgumentException("Scheduled pickup time cannot be null");
        }
        if (fulfillmentLeadTime == null) {
            throw new IllegalArgumentException("Fulfillment lead time cannot be null");
        }
        this.scheduledPickupTime = scheduledPickupTime;
        this.fulfillmentLeadTime = fulfillmentLeadTime;
        this.status = OrderStatus.SCHEDULED;
        this.domainEvents.add(
                new OrderScheduledEvent(this.orderId, scheduledPickupTime.getPickupTime()));
    }

    public void markReadyForFulfillment() {
        if (this.status != OrderStatus.SCHEDULED && this.status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Cannot mark order as ready for fulfillment in status: " + this.status);
        }
        this.status = OrderStatus.AWAITING_FULFILLMENT;
        this.domainEvents.add(new OrderReadyForFulfillmentEvent(this.orderId));
    }

    public boolean isReadyForFulfillment(LocalDateTime currentTime) {
        if (this.status != OrderStatus.SCHEDULED) {
            return false;
        }
        if (scheduledPickupTime == null || fulfillmentLeadTime == null) {
            return false;
        }
        LocalDateTime fulfillmentStartTime =
                scheduledPickupTime.calculateFulfillmentStartTime(fulfillmentLeadTime);
        return !currentTime.isBefore(fulfillmentStartTime);
    }

    public void markAsFailedToReserve(String reason) {
        if (this.status != OrderStatus.AWAITING_FULFILLMENT) {
            throw new IllegalStateException(
                    "Cannot mark order as failed to reserve in status: " + this.status);
        }
        this.status = OrderStatus.FAILED_TO_RESERVE;
    }

    public void reserveLineItem(
            String lineItemId,
            String transactionId,
            String externalReservationId,
            String warehouseId) {
        OrderLineItem lineItem = findLineItemById(lineItemId);
        lineItem.reserveItem(transactionId, externalReservationId, warehouseId);
        updateOrderStatus();
    }

    public void markLineReservationFailed(String lineItemId, String reason) {
        OrderLineItem lineItem = findLineItemById(lineItemId);
        lineItem.markReservationFailed(reason);
        updateOrderStatus();
    }

    public void commitLineItem(String lineItemId, String wesTransactionId) {
        OrderLineItem lineItem = findLineItemById(lineItemId);
        lineItem.commitItem(wesTransactionId);
        updateOrderStatus();
    }

    public void markLineCommitmentFailed(String lineItemId, String reason) {
        OrderLineItem lineItem = findLineItemById(lineItemId);
        lineItem.markCommitmentFailed(reason);
        updateOrderStatus();
    }

    public void markItemsAsPickingInProgress(List<String> skus, String pickingTaskId) {
        for (OrderLineItem item : orderLineItems) {
            if (skus.contains(item.getSku())) {
                item.markPickingInProgress(pickingTaskId);
            }
        }
    }

    public boolean isFullyReserved() {
        return orderLineItems != null
                && !orderLineItems.isEmpty()
                && orderLineItems.stream().allMatch(OrderLineItem::isReserved);
    }

    public boolean isPartiallyReserved() {
        return orderLineItems != null
                && orderLineItems.stream().anyMatch(OrderLineItem::isReserved)
                && !isFullyReserved();
    }

    public boolean hasAnyReservationFailed() {
        return orderLineItems != null
                && orderLineItems.stream().anyMatch(OrderLineItem::hasReservationFailed);
    }

    public boolean isFullyCommitted() {
        return orderLineItems != null
                && !orderLineItems.isEmpty()
                && orderLineItems.stream().allMatch(OrderLineItem::isCommitted);
    }

    public boolean isPartiallyCommitted() {
        return orderLineItems != null
                && orderLineItems.stream().anyMatch(OrderLineItem::isCommitted)
                && !isFullyCommitted();
    }

    public boolean hasAnyCommitmentFailed() {
        return orderLineItems != null
                && orderLineItems.stream().anyMatch(OrderLineItem::hasCommitmentFailed);
    }

    private OrderLineItem findLineItemById(String lineItemId) {
        if (orderLineItems == null) {
            throw new IllegalStateException("Order has no line items");
        }
        return orderLineItems.stream()
                .filter(item -> item.getLineItemId().equals(lineItemId))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Line item not found: " + lineItemId));
    }

    private void updateOrderStatus() {
        OrderStatus previousStatus = this.status;

        if (isFullyCommitted()) {
            this.status = OrderStatus.COMMITTED;
        } else if (isPartiallyCommitted()) {
            this.status = OrderStatus.PARTIALLY_COMMITTED;
        } else if (isFullyReserved()) {
            this.status = OrderStatus.RESERVED;
            if (previousStatus != OrderStatus.RESERVED) {
                this.domainEvents.add(
                        new OrderReservedEvent(this.orderId, getReservedLineItemIds()));
            }
        } else if (isPartiallyReserved()) {
            this.status = OrderStatus.PARTIALLY_RESERVED;
        }
    }

    private List<String> getReservedLineItemIds() {
        return orderLineItems.stream()
                .filter(
                        item ->
                                item.getReservationInfo() != null
                                        && item.getReservationInfo().isReserved())
                .map(OrderLineItem::getLineItemId)
                .toList();
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public ShipmentInfo getShipmentInfo() {
        return shipmentInfo;
    }

    public void setShipmentInfo(ShipmentInfo shipmentInfo) {
        this.shipmentInfo = shipmentInfo;
    }

    public List<OrderLineItem> getOrderLineItems() {
        return new ArrayList<>(orderLineItems);
    }

    public void setOrderLineItems(List<OrderLineItem> orderLineItems) {
        this.orderLineItems =
                orderLineItems != null ? new ArrayList<>(orderLineItems) : new ArrayList<>();
    }

    public void addOrderLineItem(OrderLineItem item) {
        this.orderLineItems.add(item);
    }

    public ScheduledPickupTime getScheduledPickupTime() {
        return scheduledPickupTime;
    }

    public void setScheduledPickupTime(ScheduledPickupTime scheduledPickupTime) {
        this.scheduledPickupTime = scheduledPickupTime;
    }

    public FulfillmentLeadTime getFulfillmentLeadTime() {
        return fulfillmentLeadTime;
    }

    public void setFulfillmentLeadTime(FulfillmentLeadTime fulfillmentLeadTime) {
        this.fulfillmentLeadTime = fulfillmentLeadTime;
    }

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
