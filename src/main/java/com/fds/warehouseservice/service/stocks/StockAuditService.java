package com.fds.warehouseservice.service.stocks;

import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.entity.enums.StockAuditOperationType;
import com.fds.warehouseservice.entity.enums.StockAuditReason;

import java.util.UUID;

public interface StockAuditService {

    void systemOperation(StockAuditOperationType operationType,
                         DarkstoreEntity darkstore,
                         UUID productId,
                         Integer quantityChange,
                         Integer resultingQuantity);

    void manualOperation(DarkstoreEntity darkstore,
                         UUID productId,
                         Integer quantityChange,
                         Integer resultingQuantity,
                         StockAuditReason reason,
                         String note);
}
