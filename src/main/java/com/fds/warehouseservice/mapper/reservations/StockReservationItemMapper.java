package com.fds.warehouseservice.mapper.reservations;

import com.fds.warehouseservice.dto.reservations.ReservationItemRequest;
import com.fds.warehouseservice.dto.reservations.ReservationItemResponse;
import com.fds.warehouseservice.entity.reservations.StockReservationItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StockReservationItemMapper {

    ReservationItemResponse toResponse(StockReservationItemEntity entity);
}
