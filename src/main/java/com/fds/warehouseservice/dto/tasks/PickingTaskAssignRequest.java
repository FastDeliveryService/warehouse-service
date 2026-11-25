package com.fds.warehouseservice.dto.tasks;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PickingTaskAssignRequest(
        @NotNull
        UUID workerId
) {
}
