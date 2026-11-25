package com.fds.warehouseservice.dto.reservations;

import java.util.List;
import java.util.UUID;

public record ReservationRequest(
        UUID orderId,
        UUID darkstoreId,
        List<ReservationItemRequest> items
) {
}
