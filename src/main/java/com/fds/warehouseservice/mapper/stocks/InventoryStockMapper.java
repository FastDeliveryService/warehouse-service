package com.fds.warehouseservice.mapper.stocks;

import com.fds.warehouseservice.dto.stocks.StockResponse;
import com.fds.warehouseservice.entity.stocks.InventoryStocksEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InventoryStockMapper {

    @Mapping(target = "quantity", expression = "java(entity.getQuantityOnHand() - entity.getQuantityReserved())")
    StockResponse entityToResponse(InventoryStocksEntity entity);
}
