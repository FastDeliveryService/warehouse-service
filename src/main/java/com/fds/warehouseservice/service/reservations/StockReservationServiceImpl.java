package com.fds.warehouseservice.service.reservations;

import com.fds.warehouseservice.dto.reservations.ReservationRequest;
import com.fds.warehouseservice.dto.reservations.ReservationResponse;
import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.entity.enums.ReservationStatus;
import com.fds.warehouseservice.entity.reservations.StockReservationEntity;
import com.fds.warehouseservice.entity.reservations.StockReservationItemEntity;
import com.fds.warehouseservice.mapper.reservations.StockReservationMapper;
import com.fds.warehouseservice.repository.darkstores.DarkstoreRepository;
import com.fds.warehouseservice.repository.reservations.StockReservationRepository;
import com.fds.warehouseservice.service.tasks.TaskService;
import com.fds.warehouseservice.service.stocks.InventoryStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockReservationServiceImpl implements StockReservationService {

    private final StockReservationRepository reservationRepository;
    private final StockReservationMapper mapper;
    private final DarkstoreRepository darkstoreRepository;
    private final InventoryStockService stockService;
    private final TaskService taskService;

    @Value("${order.reservation-time}")
    private Duration expirationTime;

    @Override
    public Page<ReservationResponse> getReservations(Pageable pageable) {
        Page<StockReservationEntity> reservations = reservationRepository.findAll(pageable);
        return reservations.map(mapper::toResponse);
    }

    @Override
    public ReservationResponse getReservation(UUID id) {
        StockReservationEntity reservation = reservationRepository.findById(id).orElseThrow(() ->
                new NoSuchElementException("There is no reservation with id " + id));
        return mapper.toResponse(reservation);
    }

    @Override
    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        boolean exist = reservationRepository.existsByOrderIdAndStatusIn(
                request.orderId(), List.of(ReservationStatus.ACTIVE, ReservationStatus.CONFIRMED));

        if (exist) {
            throw new IllegalStateException("There is already existing reservation for order with id " +
                    request.orderId());
        }

        DarkstoreEntity darkstore = darkstoreRepository.findById(request.darkstoreId()).orElseThrow(() ->
                new NoSuchElementException("There is no darkstore with id " + request.darkstoreId()));

        StockReservationEntity reservation = mapper.createReservationFromRequest(request);
        reservation.setDarkstore(darkstore);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setExpiresAt(OffsetDateTime.now().plus(expirationTime));

        List<StockReservationItemEntity> reservationItems = stockService.reserve(request);
        reservation.setItems(reservationItems);

        reservation = reservationRepository.save(reservation);

        return mapper.toResponse(reservation);
    }

    @Override
    @Transactional
    public ReservationResponse confirmReservation(UUID orderId) {
        StockReservationEntity reservation = reservationRepository
                .findNotExpired(orderId)
                .orElseThrow(() -> new NoSuchElementException("There is no reservation for order with id " + orderId));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return mapper.toResponse(reservation);
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        taskService.addTask(reservation);
        stockService.writeOff(reservation);

        reservation = reservationRepository.save(reservation);
        return mapper.toResponse(reservation);
    }

    @Override
    @Transactional
    public ReservationResponse cancelReservation(UUID orderId) {
        StockReservationEntity reservation = reservationRepository
                .findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("There is no reservation for order with id " + orderId));

        reservation.setStatus(ReservationStatus.CANCELLED);
        stockService.releaseReservation(reservation);

        reservation = reservationRepository.save(reservation);
        return mapper.toResponse(reservation);
    }

    @Override
    @Transactional
    public int releaseExpiredReservation() {
        List<StockReservationEntity> reservationsToRelease = reservationRepository.findExpired(OffsetDateTime.now());

        reservationsToRelease.forEach(reservation -> {
            reservation.setStatus(ReservationStatus.EXPIRED);
            stockService.releaseReservation(reservation);
        });

        return reservationsToRelease.size();
    }
}
