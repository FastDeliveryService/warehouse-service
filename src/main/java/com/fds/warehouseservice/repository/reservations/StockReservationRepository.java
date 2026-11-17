package com.fds.warehouseservice.repository.reservations;

import com.fds.warehouseservice.entity.enums.ReservationStatus;
import com.fds.warehouseservice.entity.reservations.StockReservationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends CrudRepository<StockReservationEntity, UUID> {
    Page<StockReservationEntity> findAll(Pageable pageable);

    Optional<StockReservationEntity> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    @Query("""
           SELECT e
           FROM StockReservationEntity e
           WHERE e.status = 'ACTIVE'
           AND e.expiresAt < :now
           """)
    List<StockReservationEntity> findExpired(OffsetDateTime now);
}
