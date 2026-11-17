package com.fds.warehouseservice.dto.reservations;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ReservationItemRequest(

        @NotNull
        UUID productId,

        @NotNull
        @Positive
        Integer quantity
) {
}
