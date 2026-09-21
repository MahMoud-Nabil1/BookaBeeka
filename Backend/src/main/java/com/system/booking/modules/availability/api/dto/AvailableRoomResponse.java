package com.system.booking.modules.availability.api.dto;

public record AvailableRoomResponse(
        HotelInfo hotel,
        RoomInfo room,
        StayInfo stay,
        PricingInfo pricing
) {}
