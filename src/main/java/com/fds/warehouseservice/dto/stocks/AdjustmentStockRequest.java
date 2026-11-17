package com.fds.warehouseservice.dto.stocks;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Immutable request object used to adjust the quantity of an existing stock record.
 * Captures the stock identifier, the signed quantity change to apply, the reason for
 * the adjustment, and supporting notes. All values are required, and the reason code
 * and notes must not be blank.
 */
public record AdjustmentStockRequest(

        @NotNull
        UUID stockId,

        @NotNull
        Integer quantityChange,

        @NotBlank
        String reasonCode,

        @NotBlank
        String notes
) {
}
