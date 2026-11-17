package com.fds.warehouseservice.dto.darkstores;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DarkstoreResponse(
        UUID id,
        String code,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
