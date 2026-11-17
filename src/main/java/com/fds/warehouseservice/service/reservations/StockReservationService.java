package com.fds.warehouseservice.service.reservations;

import com.fds.warehouseservice.dto.reservations.ReservationRequest;
import com.fds.warehouseservice.dto.reservations.ReservationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StockReservationService {
    Page<ReservationResponse> getReservations(Pageable pageable);
    ReservationResponse getReservation(UUID id);
    ReservationResponse createReservation(ReservationRequest request);
    ReservationResponse confirmReservation(UUID orderId);
    ReservationResponse cancelReservation(UUID orderId);
    int releaseExpiredReservation();
}
