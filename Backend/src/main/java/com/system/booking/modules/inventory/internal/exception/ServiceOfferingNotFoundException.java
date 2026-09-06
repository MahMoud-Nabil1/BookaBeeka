package com.system.booking.modules.inventory.internal.exception;

import java.util.UUID;

public class ServiceOfferingNotFoundException extends RuntimeException {

    public ServiceOfferingNotFoundException(String message) {
        super(message);
    }

    public ServiceOfferingNotFoundException(UUID serviceOfferingId) {
        super("Service offering not found with ID: " + serviceOfferingId);
    }
}