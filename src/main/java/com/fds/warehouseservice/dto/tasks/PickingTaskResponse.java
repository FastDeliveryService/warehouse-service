package com.fds.warehouseservice.dto.tasks;

import com.fds.warehouseservice.entity.enums.PickingTaskStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PickingTaskResponse(
        UUID id,
        UUID orderId,
        UUID reservationId,
        UUID darkstoreId,
        PickingTaskStatus status,
        OffsetDateTime createdAt,
        List<PickingTaskItemResponse> items
) {
}
