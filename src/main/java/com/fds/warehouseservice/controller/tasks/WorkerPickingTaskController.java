package com.fds.warehouseservice.controller.tasks;

import com.fds.warehouseservice.dto.tasks.PickingTaskResponse;
import com.fds.warehouseservice.service.tasks.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/worker/tasks")
@RequiredArgsConstructor
public class WorkerPickingTaskController {

    private final TaskService taskService;

    @PatchMapping("/{id}/complete")
    public ResponseEntity<PickingTaskResponse> completeTask(@PathVariable UUID id) {
        PickingTaskResponse task = taskService.finishTask(id);
        return ResponseEntity.ok(task);
    }
}
