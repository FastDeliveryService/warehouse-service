package com.fds.warehouseservice.service.tasks;

import com.fds.warehouseservice.dto.tasks.PickingTaskResponse;
import com.fds.warehouseservice.dto.tasks.PickingTaskRouteResponse;
import com.fds.warehouseservice.entity.enums.PickingTaskStatus;
import com.fds.warehouseservice.entity.reservations.StockReservationEntity;
import com.fds.warehouseservice.entity.tasks.PickingTaskEntity;
import com.fds.warehouseservice.entity.tasks.PickingTaskItemEntity;
import com.fds.warehouseservice.mapper.tasks.PickingTaskMapper;
import com.fds.warehouseservice.repository.tasks.PickingTaskRepository;
import com.fds.warehouseservice.service.tasks.route.RouteProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final PickingTaskRepository taskRepository;
    private final RouteProvider routeProvider;
    private final PickingTaskMapper taskMapper;

    @Override
    public Page<PickingTaskResponse> getTasks(Pageable pageable) {
        Page<PickingTaskEntity> all = taskRepository.findAll(pageable);
        return all.map(taskMapper::toResponse);
    }

    @Override
    public PickingTaskResponse getTaskById(UUID id) {
        PickingTaskEntity task = taskRepository.findById(id).orElseThrow(() ->
                new NoSuchElementException("There is no task with id: " + id));
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void addTask(StockReservationEntity reservation) {
        PickingTaskEntity task = PickingTaskEntity.builder()
                .orderId(reservation.getOrderId())
                .darkstore(reservation.getDarkstore())
                .reservation(reservation)
                .status(PickingTaskStatus.PENDING)
                .build();

        reservation.getItems().forEach(item -> {
            PickingTaskItemEntity taskItem = PickingTaskItemEntity.builder()
                    .pickingTask(task)
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .build();

            task.getPickingTaskItems().add(taskItem);
        });

        taskRepository.save(task);
    }

    @Override
    public PickingTaskRouteResponse assignTask(UUID workerId) {
        PickingTaskEntity task = taskRepository.findFirstByStatusOrderByCreatedAt(PickingTaskStatus.PENDING)
                .orElseThrow(() -> new NoSuchElementException("No free tasks available"));

        task.setStatus(PickingTaskStatus.IN_PROGRESS);
        task = taskRepository.save(task);

        String route = routeProvider.provideRoute(task);

        return taskMapper.toResponse(task, route);
    }

    @Override
    public PickingTaskResponse finishTask(UUID taskId) {
        PickingTaskEntity task = taskRepository.findById(taskId).orElseThrow(() ->
                new NoSuchElementException("There is no task with id: " + taskId));

        task.setStatus(PickingTaskStatus.READY_TO_COURIER);
        task = taskRepository.save(task);
        return taskMapper.toResponse(task);
    }
}
