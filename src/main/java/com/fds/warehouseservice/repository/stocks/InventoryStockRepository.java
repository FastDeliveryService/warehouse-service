package com.fds.warehouseservice.repository.stocks;

import com.fds.warehouseservice.entity.stocks.InventoryStocksEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStocksEntity, UUID> {

    boolean existsByProductIdAndDarkstoreId(UUID productId, UUID darkstoreId);

    Optional<InventoryStocksEntity> getByProductIdAndDarkstoreId(UUID productId, UUID darkstoreId);

    List<InventoryStocksEntity> getByDarkstoreIdAndProductIdIn(UUID darkstoreId, List<UUID> productIds);

    @Modifying
    @Query("""
            UPDATE InventoryStocksEntity e
            SET e.version = e.version + 1,
                e.quantityReserved = e.quantityReserved + :quantity,
                e.updatedAt = :currentTime
            WHERE e.productId = :productId
              AND e.darkstore.id = :darkstoreId
              AND e.quantityOnHand - e.quantityReserved >= :quantity
            """)
    int atomicReserve(UUID productId, UUID darkstoreId, int quantity, OffsetDateTime currentTime);

    default int atomicReserve(UUID productId, UUID darkstoreId, int quantity) {
        return atomicReserve(productId, darkstoreId, quantity, OffsetDateTime.now());
    }


    @Query(value = """
            UPDATE inventory_stocks
            SET version = version + 1,
                quantity_on_hand = quantity_on_hand - :quantity,
                quantity_reserved = quantity_reserved - :quantity,
                updated_at = :currentTime
            WHERE product_id = :productId
              AND darkstore_id = :darkstoreId
              AND quantity_on_hand >= :quantity
              AND quantity_reserved >= :quantity
            RETURNING quantity_on_hand
            """, nativeQuery = true)
    Optional<Integer> atomicWriteOff(UUID productId, UUID darkstoreId, int quantity,  OffsetDateTime currentTime);

    default Optional<Integer> atomicWriteOff(UUID productId, UUID darkstoreId, int quantity) {
        return atomicWriteOff(productId, darkstoreId, quantity, OffsetDateTime.now());
    }

    @Modifying
    @Query("""
           UPDATE InventoryStocksEntity e
           SET e.version = e.version + 1,
               e.quantityReserved = e.quantityReserved - :quantity,
               e.updatedAt = :currentTime
           WHERE e.productId = :productId
             AND e.darkstore.id = :darkstoreId
             AND e.quantityReserved >= :quantity
           """)
    int atomicReservationCancel(UUID productId, UUID darkstoreId, int quantity, OffsetDateTime currentTime);

    default int atomicReservationCancel(UUID productId, UUID darkstoreId, int quantity) {
        return atomicReservationCancel(productId, darkstoreId, quantity, OffsetDateTime.now());
    }
}
