package com.wei.orchestrator.order.domain.exception;

public class InvalidOrderStatusException extends RuntimeException {

    private final String invalidValue;

    public InvalidOrderStatusException(String invalidValue) {
        super("Invalid order status: " + invalidValue);
        this.invalidValue = invalidValue;
    }

    public String getInvalidValue() {
        return invalidValue;
    }
}
