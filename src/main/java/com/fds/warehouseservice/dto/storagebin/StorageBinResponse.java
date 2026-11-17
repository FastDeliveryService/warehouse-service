package com.fds.warehouseservice.dto.storagebin;

import com.fds.warehouseservice.dto.darkstores.DarkstoreResponse;

import java.util.UUID;

public record StorageBinResponse(
         UUID id,
         String binCode,
         String zone,
         String aisle,
         String shelf,
         DarkstoreResponse darkstore
) {
}
