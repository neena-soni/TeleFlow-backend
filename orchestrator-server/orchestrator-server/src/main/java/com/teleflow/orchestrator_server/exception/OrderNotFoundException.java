package com.teleflow.orchestrator_server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an order cannot be found by ID or trackingId.
 * Mapped to HTTP 404 by GlobalExceptionHandler.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(Long orderId) {
        super("Order not found with id: " + orderId);
    }

    public OrderNotFoundException(String field, String value) {
        super("Order not found with " + field + ": " + value);
    }
}
