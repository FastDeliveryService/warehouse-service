package com.fds.warehouseservice.controller.stocks;

import com.fds.warehouseservice.dto.stocks.AdjustmentStockRequest;
import com.fds.warehouseservice.dto.stocks.StockRequest;
import com.fds.warehouseservice.dto.stocks.StockResponse;
import com.fds.warehouseservice.service.stocks.InventoryStockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryStockService inventoryStockService;

    @GetMapping
    public ResponseEntity<Page<StockResponse>> getAllStocks(Pageable pageable) {
        Page<StockResponse> stocks = inventoryStockService.getStocks(pageable);
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockResponse> getStockById(@PathVariable UUID id) {
        StockResponse stock = inventoryStockService.getStock(id);
        return ResponseEntity.ok(stock);
    }

    @PostMapping("/inbound")
    public ResponseEntity<StockResponse>  inbound(
            @RequestBody @Valid StockRequest stockRequest
    ) {
        StockResponse inbound = inventoryStockService.inbound(stockRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(inbound);
    }

    @PostMapping("/adjust")
    public ResponseEntity<StockResponse>  adjust(
            @RequestBody @Valid AdjustmentStockRequest stockRequest
    ) {
        StockResponse adjust = inventoryStockService.adjust(stockRequest);
        return ResponseEntity.ok(adjust);
    }
}
