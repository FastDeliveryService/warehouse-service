package com.fds.warehouseservice.dto.storagebin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StorageBinRequest(

        @NotBlank
        @Size(max = 100)
        String binCode,

        @NotBlank
        @Size(max = 100)
        String zone,

        @NotBlank
        @Size(max = 100)
        String aisle,

        @NotBlank
        @Size(max = 100)
        String shelf
) {
}
