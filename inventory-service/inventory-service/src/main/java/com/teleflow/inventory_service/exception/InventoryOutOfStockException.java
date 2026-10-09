package com.teleflow.inventory_service.exception;

public class InventoryOutOfStockException extends RuntimeException {
    public InventoryOutOfStockException(String message) {
        super(message);
    }
}
