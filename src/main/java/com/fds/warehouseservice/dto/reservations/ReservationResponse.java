package com.fds.warehouseservice.dto.reservations;

import com.fds.warehouseservice.dto.darkstores.DarkstoreResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
             UUID id,
             UUID orderId,
             DarkstoreResponse darkstore,
             String status,
             OffsetDateTime expiresAt,
             List<ReservationItemResponse> items
) {
}
