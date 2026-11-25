package com.fds.warehouseservice.service.stocks;

import com.fds.warehouseservice.dto.reservations.ReservationItemRequest;
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

        if (!stock.getStorageBin().getId().equals(inboundRequest.storageBinId())) {
            throw new IllegalArgumentException(
                    String.format("Provided storage bin ID (%s) is not matching existing stock's bin ID.",
                            inboundRequest.storageBinId())
            );
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

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<StockReservationItemEntity> reserve(ReservationRequest request) {
        UUID darkstoreId = request.darkstoreId();

        return request.items().stream()
                .sorted(Comparator.comparing(ReservationItemRequest::productId))
                .map(item -> {
                    UUID productId = item.productId();
                    Integer quantity = item.quantity();

                    int updatedRows = stockRepository.atomicReserve(productId, darkstoreId, quantity);
                    if (updatedRows == 0) {
                        if (!stockRepository.existsByProductIdAndDarkstoreId(productId, darkstoreId)) {
                            throw new NoSuchElementException(
                                    String.format("There is no product with id %s at darkstore with id %s",
                                            productId, darkstoreId));
                        }
                        throw new NotEnoughStockException(productId, darkstoreId);
                    }


                    StockReservationItemEntity reservationItem = new StockReservationItemEntity();
                    reservationItem.setQuantity(quantity);
                    reservationItem.setProductId(productId);
                    return reservationItem;
                }).toList();
    }


    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseReservation(StockReservationEntity reservation) {
        UUID darkstoreId = reservation.getDarkstore().getId();

        reservation.getItems().stream()
                .sorted(Comparator.comparing(StockReservationItemEntity::getProductId))
                .forEach(item -> {
                    UUID productId = item.getProductId();
                    int quantity = item.getQuantity();

                    int rowsEdited = stockRepository.atomicReservationCancel(
                            productId,
                            darkstoreId,
                            quantity
                    );
                    if (rowsEdited == 0) {
                        if (stockRepository.existsByProductIdAndDarkstoreId(productId, darkstoreId)) {
                            throw new NoSuchElementException(String.format(
                                    "There is no product with id %s at darkstore with id %s",
                                    productId, darkstoreId));
                        } else {
                            throw new IllegalStateException(String.format(
                                    "Can't release more (%d), then reserved for reservation id %s",
                                    quantity, reservation.getId()));
                        }
                    }
                });
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void writeOff(StockReservationEntity reservation) {
        DarkstoreEntity darkstore = reservation.getDarkstore();

        reservation.getItems().stream()
                .sorted(Comparator.comparing(StockReservationItemEntity::getProductId))
                .forEach(item -> {
                    UUID productId = item.getProductId();
                    int quantity = item.getQuantity();

                    int resultingQuantity = stockRepository.atomicWriteOff(productId, darkstore.getId(), quantity)
                            .orElseThrow(() -> new IllegalStateException(
                                    String.format("Cannot write off %d of product %s at darkstore %s: " +
                                                    "stock not found or insufficient onHand/reserved",
                                            quantity, productId, darkstore.getId())));

                    stockAuditService.systemOperation(
                            StockAuditOperationType.OUTBOUND,
                            darkstore,
                            productId,
                            quantity,
                            resultingQuantity
                    );
                });
    }
}
