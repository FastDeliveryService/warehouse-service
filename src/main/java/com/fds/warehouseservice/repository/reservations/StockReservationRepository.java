package com.fds.warehouseservice.repository.reservations;

import com.fds.warehouseservice.entity.enums.ReservationStatus;
import com.fds.warehouseservice.entity.reservations.StockReservationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends CrudRepository<StockReservationEntity, UUID> {
    Page<StockReservationEntity> findAll(Pageable pageable);

    Optional<StockReservationEntity> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    Optional<StockReservationEntity> findByOrderIdAndStatusIn(UUID orderId, Collection<ReservationStatus> statuses);

    default Optional<StockReservationEntity> findNotExpired(UUID orderId) {
        return findByOrderIdAndStatusIn(orderId, List.of(ReservationStatus.ACTIVE, ReservationStatus.CONFIRMED));
    }

    @EntityGraph(attributePaths = "items")
    Optional<StockReservationEntity> getByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    @Query("""
           SELECT e
           FROM StockReservationEntity e
           WHERE e.status = 'ACTIVE'
           AND e.expiresAt < :now
           """)
    List<StockReservationEntity> findExpired(OffsetDateTime now);
}
