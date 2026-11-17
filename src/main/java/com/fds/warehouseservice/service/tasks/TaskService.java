package com.fds.warehouseservice.service.tasks;

import com.fds.warehouseservice.dto.tasks.PickingTaskResponse;
import com.fds.warehouseservice.dto.tasks.PickingTaskRouteResponse;
import com.fds.warehouseservice.entity.reservations.StockReservationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TaskService {

    Page<PickingTaskResponse> getTasks(Pageable pageable);

    PickingTaskResponse getTaskById(UUID id);

    void addTask(StockReservationEntity reservation);

    PickingTaskRouteResponse assignTask(UUID workerId);

    PickingTaskResponse finishTask(UUID taskId);
}
