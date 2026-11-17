package com.fds.warehouseservice.service.stocks;

import com.fds.warehouseservice.dto.reservations.ReservationRequest;
import com.fds.warehouseservice.dto.stocks.AdjustmentStockRequest;
import com.fds.warehouseservice.dto.stocks.StockRequest;
import com.fds.warehouseservice.dto.stocks.StockResponse;
import com.fds.warehouseservice.entity.reservations.StockReservationEntity;
import com.fds.warehouseservice.entity.reservations.StockReservationItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface InventoryStockService {
    Page<StockResponse> getStocks(Pageable pageable);
    StockResponse getStock(UUID stockId);
    StockResponse inbound(StockRequest inboundRequest);
    StockResponse adjust(AdjustmentStockRequest adjustmentRequest);
    List<StockReservationItemEntity> reserve(ReservationRequest request);
    void releaseReservation(StockReservationEntity reservation);
    void writeOff(StockReservationEntity reservation);


//    boolean checkAvailability(ReservationRequest request); //todo delete
}
