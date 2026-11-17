package com.fds.warehouseservice.repository.stocks;

import com.fds.warehouseservice.entity.stocks.StockAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StockAuditLogRepository extends JpaRepository<StockAuditLogEntity, UUID> {
}
