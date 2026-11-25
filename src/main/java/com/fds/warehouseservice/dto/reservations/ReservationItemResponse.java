package com.fds.warehouseservice.dto.reservations;

import java.util.UUID;

public record ReservationItemResponse(
        UUID id,
        UUID productId,
        Integer quantity
) {
}
