package com.fds.warehouseservice.service.stocks;

import com.fasterxml.uuid.Generators;
import com.fds.security.utils.SecurityUtils;
import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.entity.enums.StockAuditOperationType;
import com.fds.warehouseservice.entity.enums.StockAuditReason;
import com.fds.warehouseservice.entity.stocks.StockAuditLogEntity;
import com.fds.warehouseservice.repository.stocks.StockAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockAuditServiceImpl implements StockAuditService {

    private final StockAuditLogRepository stockAuditLogRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void systemOperation(StockAuditOperationType operationType,
                                DarkstoreEntity darkstore,
                                UUID productId,
                                Integer quantityChange,
                                Integer resultingQuantity) {
        StockAuditLogEntity logEntity = StockAuditLogEntity.builder()
                .id(Generators.timeBasedGenerator().generate())
                .darkstore(darkstore)
                .productId(productId)
                .operationType(operationType)
                .quantityChange(quantityChange)
                .resultingQuantity(resultingQuantity)
                .performedBy(SecurityUtils.getSystemId())
                .build();

        stockAuditLogRepository.save(logEntity);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void manualOperation(DarkstoreEntity darkstore,
                                UUID productId,
                                Integer quantityChange,
                                Integer resultingQuantity,
                                StockAuditReason reason,
                                String note) {
        StockAuditLogEntity logEntity = StockAuditLogEntity.builder()
                .id(Generators.timeBasedGenerator().generate())
                .darkstore(darkstore)
                .productId(productId)
                .operationType(StockAuditOperationType.ADJUSTMENT)
                .quantityChange(quantityChange)
                .resultingQuantity(resultingQuantity)
                .reasonCode(reason)
                .notes(note)
                .performedBy(SecurityUtils.getUserId())
                .build();

        stockAuditLogRepository.save(logEntity);
    }
}
