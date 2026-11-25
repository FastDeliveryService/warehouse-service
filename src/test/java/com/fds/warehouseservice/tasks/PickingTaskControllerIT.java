package com.fds.warehouseservice.tasks;

import com.fds.warehouseservice.dto.tasks.PickingTaskAssignRequest;
import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.entity.darkstores.StorageBinEntity;
import com.fds.warehouseservice.entity.enums.PickingTaskStatus;
import com.fds.warehouseservice.entity.enums.StorageBinZone;
import com.fds.warehouseservice.entity.reservations.StockReservationEntity;
import com.fds.warehouseservice.entity.enums.ReservationStatus;
import com.fds.warehouseservice.entity.tasks.PickingTaskEntity;
import com.fds.warehouseservice.entity.tasks.PickingTaskItemEntity;
import com.fds.warehouseservice.repository.reservations.StockReservationRepository;
import com.fds.warehouseservice.repository.tasks.PickingTaskRepository;
import com.fds.warehouseservice.support.ApiRequests;
import com.fds.warehouseservice.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.AllOf.allOf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the picking-task endpoints on {@code /api/v1/admin/tasks} and
 * {@code /api/v1/worker/tasks}.
 *
 * <p>Also covers {@code RouteProviderImpl}, which sorts items by bin zone then shelf. That
 * ordering is only observable through the {@code route} string returned by the assign endpoint.
 */
class PickingTaskControllerIT extends IntegrationTestBase {

    private static final String ADMIN_TASKS_ENDPOINT = "/api/v1/admin/tasks";
    private static final String WORKER_TASKS_ENDPOINT = "/api/v1/worker/tasks";

    @Autowired
    private PickingTaskRepository pickingTaskRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    // --- Test Data Builders ---

    private PickingTaskEntity createSeededTask(DarkstoreEntity darkstore, PickingTaskStatus status) {
        UUID orderId = UUID.randomUUID();

        StockReservationEntity reservation = new StockReservationEntity();
        reservation.setOrderId(orderId);
        reservation.setDarkstore(darkstore);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setExpiresAt(OffsetDateTime.now().plusHours(1));
        reservation = stockReservationRepository.save(reservation);

        PickingTaskEntity task = PickingTaskEntity.builder()
                .orderId(orderId)
                .darkstore(darkstore)
                .reservation(reservation)
                .status(status)
                .pickingTaskItems(new ArrayList<>())
                .build();

        return pickingTaskRepository.save(task);
    }

    private void seedTaskItem(PickingTaskEntity task, UUID productId, int quantity) {
        PickingTaskItemEntity item = PickingTaskItemEntity.builder()
                .pickingTask(task)
                .productId(productId)
                .quantity(quantity)
                .build();
        task.getPickingTaskItems().add(item);
        pickingTaskRepository.save(task);
    }

    // --- Admin Endpoints Tests ---

    @Test
    @DisplayName("Should successfully retrieve a paginated list of picking tasks as an Admin")
    void shouldListAllTasksAsAdmin() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        createSeededTask(darkstore, PickingTaskStatus.PENDING);
        createSeededTask(darkstore, PickingTaskStatus.IN_PROGRESS);

        ApiRequests.getJson(mockMvc, ADMIN_TASKS_ENDPOINT, jwtFactory.adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].status", hasItems("PENDING", "IN_PROGRESS")));
    }

    @Test
    @DisplayName("Should assign a PENDING task to a worker, update its status, and return a picking route")
    void shouldAssignTaskAndReturnRoute() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore, "BIN-A1", StorageBinZone.AMBIENT, "AISLE-1", "SHELF-1");

        UUID productId = UUID.randomUUID();
        givenAvailableStock(darkstore, bin, productId, "SKU-12345", 50);

        PickingTaskEntity task = createSeededTask(darkstore, PickingTaskStatus.PENDING);
        seedTaskItem(task, productId, 5);
        PickingTaskAssignRequest request = new PickingTaskAssignRequest(UUID.randomUUID());

        // Assuming the assign endpoint takes the worker assignment details in the body
        ApiRequests.patchJson(mockMvc, ADMIN_TASKS_ENDPOINT, request, jwtFactory.adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.route", allOf(
                        containsString("SKU-12345"),
                        containsString("BIN-A1"),
                        containsString("Quantity: 5"),
                        containsString("AMBIENT")
                )));

        PickingTaskEntity updatedTask = pickingTaskRepository.findById(task.getId()).orElseThrow();
        assertThat(updatedTask.getStatus()).isEqualTo(PickingTaskStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should return an error when attempting to assign a task but no PENDING tasks exist")
    void shouldFailWhenAssigningWithNoPendingTasks() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        createSeededTask(darkstore, PickingTaskStatus.IN_PROGRESS);
        PickingTaskAssignRequest request = new PickingTaskAssignRequest(UUID.randomUUID());

        ApiRequests.patchJson(mockMvc, ADMIN_TASKS_ENDPOINT, request, jwtFactory.adminToken())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should ensure tasks are not double-assigned to different workers")
    void shouldNotReassignAlreadyAssignedTasks() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        PickingTaskAssignRequest request = new PickingTaskAssignRequest(UUID.randomUUID());

        UUID productId = UUID.randomUUID();
        givenAvailableStock(darkstore, bin, productId, "SKU-999", 10);

        PickingTaskEntity task1 = createSeededTask(darkstore, PickingTaskStatus.PENDING);
        seedTaskItem(task1, productId, 2);

        // First assignment should succeed
        ApiRequests.patchJson(mockMvc, ADMIN_TASKS_ENDPOINT, request, jwtFactory.adminToken())
                .andExpect(status().isOk());

        // Second assignment should fail because the only task is now IN_PROGRESS
        ApiRequests.patchJson(mockMvc, ADMIN_TASKS_ENDPOINT, request, jwtFactory.adminToken())
                .andExpect(status().isNotFound());

        List<PickingTaskEntity> remainingPending = pickingTaskRepository.findAll().stream()
                .filter(t -> t.getStatus() == PickingTaskStatus.PENDING)
                .toList();
        assertThat(remainingPending).isEmpty();
    }

    // --- Worker Endpoints Tests ---

    @Test
    @DisplayName("Should successfully mark an IN_PROGRESS task as READY_TO_COURIER as a Worker")
    void shouldCompleteTaskAsWorker() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        PickingTaskEntity task = createSeededTask(darkstore, PickingTaskStatus.IN_PROGRESS);

        String completeEndpoint = WORKER_TASKS_ENDPOINT + "/" + task.getId() + "/complete";

        ApiRequests.patchEmpty(mockMvc, completeEndpoint, jwtFactory.workerToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_TO_COURIER"));

        PickingTaskEntity updatedTask = pickingTaskRepository.findById(task.getId()).orElseThrow();
        assertThat(updatedTask.getStatus()).isEqualTo(PickingTaskStatus.READY_TO_COURIER);
    }

    @Test
    @DisplayName("Should return an error when a Worker attempts to complete a non-existent task")
    void shouldFailToCompleteUnknownTask() throws Exception {
        String completeEndpoint = WORKER_TASKS_ENDPOINT + "/" + UUID.randomUUID() + "/complete";

        ApiRequests.patchEmpty(mockMvc, completeEndpoint, jwtFactory.workerToken())
                .andExpect(status().isNotFound());
    }
}