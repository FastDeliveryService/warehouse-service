package com.fds.warehouseservice.controller.reservations;

import com.fds.warehouseservice.dto.reservations.ReservationResponse;
import com.fds.warehouseservice.service.reservations.StockReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reservations")
@RequiredArgsConstructor
public class AdminReservationController {

    private final StockReservationService reservationService;

    @GetMapping
    public ResponseEntity<Page<ReservationResponse>> getAllReservations(Pageable pageable) {
        Page<ReservationResponse> reservations = reservationService.getReservations(pageable);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable UUID id) {
        ReservationResponse reservation = reservationService.getReservation(id);
        return ResponseEntity.ok(reservation);
    }
}