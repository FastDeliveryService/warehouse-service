package com.fds.warehouseservice.controller.tasks;

import com.fds.warehouseservice.dto.tasks.PickingTaskAssignRequest;
import com.fds.warehouseservice.dto.tasks.PickingTaskResponse;
import com.fds.warehouseservice.dto.tasks.PickingTaskRouteResponse;
import com.fds.warehouseservice.service.tasks.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tasks")
@RequiredArgsConstructor
public class AdminPickingTaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<Page<PickingTaskResponse>> getTasks(Pageable pageable) {
        Page<PickingTaskResponse> tasks = taskService.getTasks(pageable);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PickingTaskResponse> getTaskById(@PathVariable UUID id) {
        PickingTaskResponse task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    @PatchMapping
    public ResponseEntity<PickingTaskRouteResponse> assignTasks(
            @RequestBody @Valid PickingTaskAssignRequest request
    ) {
        PickingTaskRouteResponse pickingTaskRouteResponse = taskService.assignTask(request.workerId());
        return ResponseEntity.ok(pickingTaskRouteResponse);
    }
}