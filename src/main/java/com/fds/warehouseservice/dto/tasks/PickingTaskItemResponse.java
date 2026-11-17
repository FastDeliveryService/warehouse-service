package com.fds.warehouseservice.dto.tasks;

import java.util.UUID;

public record PickingTaskItemResponse(
        UUID id,
        UUID productId,
        int quantity
) {
}
