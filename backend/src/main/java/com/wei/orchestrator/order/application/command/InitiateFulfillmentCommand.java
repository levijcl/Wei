package com.wei.orchestrator.order.application.command;

public class InitiateFulfillmentCommand {
    private String orderId;

    public InitiateFulfillmentCommand() {}

    public InitiateFulfillmentCommand(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}
