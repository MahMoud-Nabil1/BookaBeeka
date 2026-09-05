package com.system.booking.modules.inventory.internal.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(UUID resourceId) {
        super("Resource not found with ID: " + resourceId);
    }
}