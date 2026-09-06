package com.system.booking.modules.inventory.internal.exception;

import java.util.UUID;

public class AmenityNotFoundException extends RuntimeException {

    public AmenityNotFoundException(String message) {
        super(message);
    }

    public AmenityNotFoundException(UUID amenityId) {
        super("Amenity not found with ID: " + amenityId);
    }
}