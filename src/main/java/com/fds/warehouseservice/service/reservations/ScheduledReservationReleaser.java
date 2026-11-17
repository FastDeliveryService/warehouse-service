package com.fds.warehouseservice.service.reservations;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class ScheduledReservationReleaser {

    private final StockReservationService reservationService;

    @Scheduled(fixedRate = 60000)
    public void scheduledReservationReleaser() {
        int released = reservationService.releaseExpiredReservation();

        log.info("Scheduled reservation released: {}", released);
    }
}
