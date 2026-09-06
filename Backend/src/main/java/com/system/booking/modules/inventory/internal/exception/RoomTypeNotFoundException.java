package com.system.booking.modules.inventory.internal.exception;

import java.util.UUID;

public class RoomTypeNotFoundException extends RuntimeException {

    public RoomTypeNotFoundException(String message) {
        super(message);
    }

    public RoomTypeNotFoundException(UUID roomTypeId) {
        super("Room type not found with ID: " + roomTypeId);
    }
}