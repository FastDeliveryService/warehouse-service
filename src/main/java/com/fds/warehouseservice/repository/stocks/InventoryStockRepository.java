package com.fds.warehouseservice.repository.stocks;

import com.fds.warehouseservice.entity.stocks.InventoryStocksEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStocksEntity, UUID> {
    Optional<InventoryStocksEntity> getByProductIdAndDarkstoreId(
            UUID productId,
            UUID darkstoreId
    );

    List<InventoryStocksEntity> getByDarkstoreIdAndProductIdIn(UUID darkstoreId, List<UUID> productIds);
}
