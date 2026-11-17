package com.fds.warehouseservice.dto.stocks;

import com.fds.warehouseservice.dto.darkstores.DarkstoreResponse;
import com.fds.warehouseservice.dto.storagebin.StorageBinResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StockResponse(
        UUID id,
        UUID productId,
        String sku,
        Integer quantity,
        DarkstoreResponse darkstore,
        StorageBinResponse storageBin,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
