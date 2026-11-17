package com.fds.warehouseservice.dto.tasks;

import com.fds.warehouseservice.entity.enums.PickingTaskStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PickingTaskRouteResponse(
        UUID id,
        UUID orderId,
        PickingTaskStatus status,
        OffsetDateTime createdAt,
        String route
) {
}
