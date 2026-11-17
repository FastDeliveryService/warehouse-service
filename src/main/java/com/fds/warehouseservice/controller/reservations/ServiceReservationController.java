package com.fds.warehouseservice.controller.reservations;

import com.fds.warehouseservice.dto.reservations.ReservationRequest;
import com.fds.warehouseservice.dto.reservations.ReservationResponse;
import com.fds.warehouseservice.service.reservations.StockReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service")
@RequiredArgsConstructor
public class ServiceReservationController {

    private final StockReservationService reservationService;

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> createReservation(
            @RequestBody @Valid ReservationRequest reservationRequest
    ) {
        ReservationResponse reservation = reservationService.createReservation(reservationRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    @PostMapping("{orderId}/confirm")
    public ResponseEntity<ReservationResponse> confirmReservation(
            @PathVariable UUID orderId
    ) {
        ReservationResponse confirmed = reservationService.confirmReservation(orderId);
        return ResponseEntity.ok(confirmed);
    }

    @PostMapping("{orderId}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable UUID orderId
    ) {
        ReservationResponse canceled = reservationService.cancelReservation(orderId);
        return ResponseEntity.ok(canceled);
    }
}
