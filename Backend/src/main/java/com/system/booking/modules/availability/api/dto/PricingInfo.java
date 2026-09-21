package com.system.booking.modules.availability.api.dto;

import java.math.BigDecimal;

public record PricingInfo(
        BigDecimal pricePerNight,
        BigDecimal totalPrice,
        String currency
) {}
