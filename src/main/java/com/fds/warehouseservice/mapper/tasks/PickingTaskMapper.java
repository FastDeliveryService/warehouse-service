package com.fds.warehouseservice.mapper.tasks;

import com.fds.warehouseservice.dto.tasks.PickingTaskItemResponse;
import com.fds.warehouseservice.dto.tasks.PickingTaskResponse;
import com.fds.warehouseservice.dto.tasks.PickingTaskRouteResponse;
import com.fds.warehouseservice.entity.tasks.PickingTaskEntity;
import com.fds.warehouseservice.entity.tasks.PickingTaskItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface PickingTaskMapper {

    @Mapping(target = "reservationId", source = "reservation.id")
    @Mapping(target = "darkstoreId", source = "darkstore.id")
    @Mapping(target = "items", source = "pickingTaskItems")
    PickingTaskResponse toResponse(PickingTaskEntity task);

    PickingTaskRouteResponse toResponse(PickingTaskEntity entity, String route);

    PickingTaskItemResponse itemToResponse(PickingTaskItemEntity entity);
}
