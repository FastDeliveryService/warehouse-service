package com.fds.warehouseservice.mapper.reservations;

import com.fds.warehouseservice.dto.reservations.ReservationRequest;
import com.fds.warehouseservice.dto.reservations.ReservationResponse;
import com.fds.warehouseservice.entity.reservations.StockReservationEntity;
import com.fds.warehouseservice.mapper.darkstores.DarkstoreMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {StockReservationItemMapper.class, DarkstoreMapper.class})
public interface StockReservationMapper {

    ReservationResponse toResponse(StockReservationEntity entity);

    @Mapping(target = "id" , ignore = true)
    @Mapping(target = "darkstore", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "items",  ignore = true)
    StockReservationEntity createReservationFromRequest(ReservationRequest request);
}
