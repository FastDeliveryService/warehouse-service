package com.fds.warehouseservice.dto.stocks;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record StockRequest(

        @NotNull
        UUID productId,

        @NotBlank
        String sku,

        @NotNull
        @Positive
        Integer quantity,

        @NotNull
        UUID darkstoreId,

        @NotNull
        UUID storageBinId
) {
}
