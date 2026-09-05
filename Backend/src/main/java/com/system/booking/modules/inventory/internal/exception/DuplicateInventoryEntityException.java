package com.system.booking.modules.inventory.internal.exception;

public class DuplicateInventoryEntityException extends RuntimeException {

    public DuplicateInventoryEntityException(String message) {
        super(message);
    }
}