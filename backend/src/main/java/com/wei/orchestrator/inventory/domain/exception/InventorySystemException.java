package com.wei.orchestrator.inventory.domain.exception;

public class InventorySystemException extends RuntimeException {

    public InventorySystemException(String message) {
        super(message);
    }

    public InventorySystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
