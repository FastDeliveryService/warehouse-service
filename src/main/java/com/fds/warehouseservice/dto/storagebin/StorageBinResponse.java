package com.fds.warehouseservice.dto.storagebin;

import java.util.UUID;

public record StorageBinResponse(
         UUID id,
         String binCode,
         String zone,
         String aisle,
         String shelf,
         UUID darkstoreId
) {
}
