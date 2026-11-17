package com.fds.warehouseservice.service.stocks;

import com.fds.warehouseservice.dto.reservations.ReservationRequest;
import com.fds.warehouseservice.dto.stocks.AdjustmentStockRequest;
import com.fds.warehouseservice.dto.stocks.StockRequest;
import com.fds.warehouseservice.dto.stocks.StockResponse;
import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.entity.darkstores.StorageBinEntity;
import com.fds.warehouseservice.entity.enums.StockAuditOperationType;
import com.fds.warehouseservice.entity.enums.StockAuditReason;
import com.fds.warehouseservice.entity.reservations.StockReservationEntity;
import com.fds.warehouseservice.entity.reservations.StockReservationItemEntity;
import com.fds.warehouseservice.entity.stocks.InventoryStocksEntity;
import com.fds.warehouseservice.exceptions.NotEnoughStockException;
import com.fds.warehouseservice.mapper.stocks.InventoryStockMapper;
import com.fds.warehouseservice.repository.darkstores.DarkstoreRepository;
import com.fds.warehouseservice.repository.darkstores.StorageBinRepository;
import com.fds.warehouseservice.repository.stocks.InventoryStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class InventoryStockServiceImpl implements InventoryStockService {

    private final InventoryStockRepository stockRepository;
    private final StockAuditService stockAuditService;
    private final InventoryStockMapper stockMapper;
    private final DarkstoreRepository darkstoreRepository;
    private final StorageBinRepository binRepository;

    @Override
    public Page<StockResponse> getStocks(Pageable pageable) {
        Page<InventoryStocksEntity> all = stockRepository.findAll(pageable);
        return all.map(stockMapper::entityToResponse);
    }

    @Override
    public StockResponse getStock(UUID stockId) {
        InventoryStocksEntity stock = stockRepository.findById(stockId).orElseThrow(() ->
                new NoSuchElementException("There is no stock with id: " + stockId));
        return stockMapper.entityToResponse(stock);
    }

    @Override
    @Transactional
    public StockResponse inbound(StockRequest inboundRequest) {
        Optional<InventoryStocksEntity> isExist = stockRepository
                .getByProductIdAndDarkstoreId(
                        inboundRequest.productId(),
                        inboundRequest.darkstoreId());

        InventoryStocksEntity stock;

        if (isExist.isEmpty()) {
            DarkstoreEntity darkstore = darkstoreRepository
                    .findByIdAndActiveTrue(inboundRequest.darkstoreId()).orElseThrow(() ->
                            new NoSuchElementException("There is no darkstore with id " + inboundRequest.darkstoreId()));

            StorageBinEntity bin = binRepository
                    .findByIdAndDarkstoreIdAndActiveTrue(inboundRequest.storageBinId(), darkstore.getId()).orElseThrow(() ->
                            new NoSuchElementException("There is no bin with id " + inboundRequest.storageBinId()));


            stock = new InventoryStocksEntity();
            stock.setDarkstore(darkstore);
            stock.setStorageBin(bin); //todo what to do with bin????
            stock.setProductId(inboundRequest.productId());
            stock.setSku(inboundRequest.sku());
        } else {
            stock = isExist.get();
        }

        int startingQuantity = stock.getQuantityOnHand() - stock.getQuantityReserved();
        stock.setQuantityOnHand(stock.getQuantityOnHand() + inboundRequest.quantity());

        stockRepository.save(stock);

        stockAuditService.systemOperation(
                StockAuditOperationType.INBOUND,
                stock.getDarkstore(),
                stock.getProductId(),
                inboundRequest.quantity(),
                startingQuantity + inboundRequest.quantity()
        );

        return stockMapper.entityToResponse(stock);
    }

    @Override
    @Transactional
    public StockResponse adjust(AdjustmentStockRequest adjustmentRequest) {
        StockAuditReason reason = StockAuditReason.fromStringCaseIgnore(adjustmentRequest.reasonCode());
        InventoryStocksEntity stock = stockRepository.findById(adjustmentRequest.stockId()).orElseThrow(() ->
                new NoSuchElementException("There is no inventory stock with id " + adjustmentRequest.stockId()));
        int startingQuantity = stock.getQuantityOnHand() - stock.getQuantityReserved();

        stock.setQuantityOnHand(stock.getQuantityOnHand() + adjustmentRequest.quantityChange());
        stockRepository.save(stock);

        stockAuditService.manualOperation(
                stock.getDarkstore(),
                stock.getProductId(),
                adjustmentRequest.quantityChange(),
                startingQuantity + adjustmentRequest.quantityChange(),
                reason,
                adjustmentRequest.notes()
        );

        return stockMapper.entityToResponse(stock);
    }

    @Override //todo retry
    @Transactional(propagation = Propagation.MANDATORY)
    public List<StockReservationItemEntity> reserve(ReservationRequest request) {
        List<StockReservationItemEntity> result = new ArrayList<>();

        request.items().forEach(item -> {
            UUID productId = item.productId();
            Integer quantity = item.quantity();

            InventoryStocksEntity availableStock = getStock(productId, request.darkstoreId());
            int available = availableStock.getQuantityOnHand() - availableStock.getQuantityReserved();

            if (available < quantity) {
                throw new NotEnoughStockException(productId, request.darkstoreId());
            }

            StockReservationItemEntity reservationItem = new StockReservationItemEntity();
            reservationItem.setQuantity(quantity);
            reservationItem.setProductId(productId);
            result.add(reservationItem);
        });
        return result;
    }


    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseReservation(StockReservationEntity reservation) {
        reservation.getItems().forEach(item -> {
            InventoryStocksEntity stock = getStock(item.getProductId(), reservation.getDarkstore().getId());
            stock.setQuantityReserved(stock.getQuantityReserved() - item.getQuantity());
        });
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void writeOff(StockReservationEntity reservation) {
        reservation.getItems().forEach(item -> {
            InventoryStocksEntity stocks = getStock(item.getProductId(), reservation.getDarkstore().getId());

            stocks.setQuantityOnHand(stocks.getQuantityOnHand() - item.getQuantity());
            stocks.setQuantityReserved(stocks.getQuantityReserved() - item.getQuantity());
            stockRepository.save(stocks);

            stockAuditService.systemOperation(
                    StockAuditOperationType.OUTBOUND,
                    reservation.getDarkstore(),
                    item.getProductId(),
                    item.getQuantity(),
                    stocks.getQuantityOnHand()
            );
        });
    }

    private InventoryStocksEntity getStock(UUID productId, UUID darkstoreId) {
        return stockRepository.getByProductIdAndDarkstoreId(productId, darkstoreId).orElseThrow(() ->
                new NoSuchElementException(String.format("There is no product with id %s at darkstore with id %s",
                        productId, darkstoreId)));
    }
}
