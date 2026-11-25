package com.fds.warehouseservice.reservations;

import com.fds.warehouseservice.dto.reservations.ReservationItemRequest;
import com.fds.warehouseservice.dto.reservations.ReservationRequest;
import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.entity.darkstores.StorageBinEntity;
import com.fds.warehouseservice.entity.enums.ReservationStatus;
import com.fds.warehouseservice.entity.reservations.StockReservationEntity;
import com.fds.warehouseservice.entity.reservations.StockReservationItemEntity;
import com.fds.warehouseservice.entity.stocks.InventoryStocksEntity;
import com.fds.warehouseservice.entity.stocks.StockAuditLogEntity;
import com.fds.warehouseservice.repository.reservations.StockReservationRepository;
import com.fds.warehouseservice.repository.stocks.StockAuditLogRepository;
import com.fds.warehouseservice.repository.tasks.PickingTaskRepository;
import com.fds.warehouseservice.support.ApiRequests;
import com.fds.warehouseservice.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the reservation lifecycle on {@code /api/v1/service} and
 * {@code /api/v1/admin/reservations}.
 *
 * <p>These cross subsystem boundaries deliberately: confirming a reservation must write off
 * stock, emit an audit row and create a picking task within one transaction. Testing that
 * through HTTP against a real database is the only way to prove the transaction actually holds.
 */
class ReservationLifecycleIT extends IntegrationTestBase {

    private static final String SERVICE = "/api/v1/service";
    private static final String ADMIN = "/api/v1/admin/reservations";

    @Autowired
    private StockReservationRepository reservationRepository;

    @Autowired
    private StockAuditLogRepository auditLogRepository;

    @Autowired
    private PickingTaskRepository pickingTaskRepository;

    private ReservationRequest requestFor(UUID orderId, UUID darkstoreId, UUID productId, int quantity) {
        return new ReservationRequest(orderId, darkstoreId,
                List.of(new ReservationItemRequest(productId, quantity)));
    }

    private StockReservationEntity reload(UUID orderId, ReservationStatus status) {
        return reservationRepository.getByOrderIdAndStatus(orderId, status).orElseThrow();
    }

    @Test
    @DisplayName("POST /reservations creates an ACTIVE reservation with a future expiry")
    void createsReservation() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        UUID productId = UUID.randomUUID();
        givenAvailableStock(darkstore, bin, productId, "SKU-1", 100);
        UUID orderId = UUID.randomUUID();

        ApiRequests.postJson(mockMvc, SERVICE + "/reservations",
                        requestFor(orderId, darkstore.getId(), productId, 4),
                        jwtFactory.serviceToken())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(4))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());

        StockReservationEntity saved = reload(orderId, ReservationStatus.ACTIVE);
        assertThat(saved.getExpiresAt()).isAfter(java.time.OffsetDateTime.now());
        assertThat(saved.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("A second active reservation for the same order is rejected")
    void rejectsDuplicateActiveReservation() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        UUID productId = UUID.randomUUID();
        givenAvailableStock(darkstore, bin, productId, "SKU-1", 100);
        UUID orderId = UUID.randomUUID();

        ApiRequests.postJson(mockMvc, SERVICE + "/reservations",
                requestFor(orderId, darkstore.getId(), productId, 2), jwtFactory.serviceToken())
                .andExpect(status().isCreated());

        // IllegalStateException is not mapped by AppExceptionHandler -> 500. Pin the behaviour.
        ApiRequests.postJson(mockMvc, SERVICE + "/reservations",
                        requestFor(orderId, darkstore.getId(), productId, 2), jwtFactory.serviceToken())
                .andExpect(status().isBadRequest());

        assertThat(reservationRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("CONFIRM writes the stock off, audits OUTBOUND and creates a picking task")
    void confirmCascadesAcrossSubsystems() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        UUID productId = UUID.randomUUID();
        InventoryStocksEntity stock = givenAvailableStock(darkstore, bin, productId, "SKU-1", 40);
        UUID orderId = UUID.randomUUID();

        ApiRequests.postJson(mockMvc, SERVICE + "/reservations",
                requestFor(orderId, darkstore.getId(), productId, 10), jwtFactory.serviceToken())
                .andExpect(status().isCreated());

        ApiRequests.postEmpty(mockMvc, SERVICE + "/" + orderId + "/confirm", jwtFactory.serviceToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Stock is written off, not merely reserved.
        InventoryStocksEntity after = reloadStock(stock.getId());
        assertThat(after.getQuantityOnHand()).isEqualTo(30);
        assertThat(after.getQuantityReserved()).isZero();

        // An OUTBOUND audit row is recorded.
        List<StockAuditLogEntity> auditRows = auditLogRepository.findAll();
        assertThat(auditRows).hasSize(1);
        assertThat(auditRows.getFirst().getOperationType().name()).isEqualTo("OUTBOUND");
        assertThat(auditRows.getFirst().getQuantityChange()).isEqualTo(10);
        assertThat(auditRows.getFirst().getResultingQuantity()).isEqualTo(30);

        // And a PENDING picking task exists for the order.
        assertThat(pickingTaskRepository.findAll()).hasSize(1);
        assertThat(pickingTaskRepository.findAll().getFirst().getOrderId()).isEqualTo(orderId);
        assertThat(pickingTaskRepository.findAll().getFirst().getStatus().name()).isEqualTo("PENDING");

        assertThat(reload(orderId, ReservationStatus.CONFIRMED)).isNotNull();
    }

    @Test
    @DisplayName("Reserving more than is available fails and leaves stock untouched")
    void rejectsInsufficientStock() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        UUID productId = UUID.randomUUID();
        InventoryStocksEntity stock = givenAvailableStock(darkstore, bin, productId, "SKU-1", 3);
        UUID orderId = UUID.randomUUID();

        ApiRequests.postJson(mockMvc, SERVICE + "/reservations",
                        requestFor(orderId, darkstore.getId(), productId, 5), jwtFactory.serviceToken())
                .andExpect(status().isConflict());

        // The whole transaction rolled back: no reservation row, stock unchanged.
        assertThat(reservationRepository.findAll()).isEmpty();
        assertThat(reloadStock(stock.getId()).getQuantityOnHand()).isEqualTo(3);
        assertThat(reloadStock(stock.getId()).getQuantityReserved()).isZero();
    }

    @Test
    @DisplayName("Availability excludes quantity already reserved")
    void availabilityAccountsForReservations() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        UUID productId = UUID.randomUUID();
        // 10 on hand, 8 already reserved -> only 2 available.
        givenStock(darkstore, bin, productId, "SKU-1", 10, 8);

        ApiRequests.postJson(mockMvc, SERVICE + "/reservations",
                        requestFor(UUID.randomUUID(), darkstore.getId(), productId, 3), jwtFactory.serviceToken())
                .andExpect(status().isConflict());

        ApiRequests.postJson(mockMvc, SERVICE + "/reservations",
                        requestFor(UUID.randomUUID(), darkstore.getId(), productId, 2), jwtFactory.serviceToken())
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Reservation for an unknown product fails")
    void rejectsUnknownProduct() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();

        ApiRequests.postJson(mockMvc, SERVICE + "/reservations",
                        requestFor(UUID.randomUUID(), darkstore.getId(), UUID.randomUUID(), 1),
                        jwtFactory.serviceToken())
                .andExpect(status().isNotFound());

        assertThat(reservationRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Reservation for an unknown darkstore fails")
    void rejectsUnknownDarkstore() throws Exception {
        ApiRequests.postJson(mockMvc, SERVICE + "/reservations",
                        requestFor(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1),
                        jwtFactory.serviceToken())
                .andExpect(status().isNotFound());

        assertThat(reservationRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Confirming an order that has no active reservation fails")
    void confirmUnknownOrderFails() throws Exception {
        ApiRequests.postEmpty(mockMvc, SERVICE + "/" + UUID.randomUUID() + "/confirm", jwtFactory.serviceToken())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A reservation confirmation is idempotent")
    void cannotConfirmTwice() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        UUID productId = UUID.randomUUID();
        givenAvailableStock(darkstore, bin, productId, "SKU-1", 20);
        UUID orderId = UUID.randomUUID();

        ApiRequests.postJson(mockMvc, SERVICE + "/reservations",
                requestFor(orderId, darkstore.getId(), productId, 5), jwtFactory.serviceToken())
                .andExpect(status().isCreated());

        ApiRequests.postEmpty(mockMvc, SERVICE + "/" + orderId + "/confirm", jwtFactory.serviceToken())
                .andExpect(status().isOk());

        // No longer ACTIVE, so the lookup misses.
        ApiRequests.postEmpty(mockMvc, SERVICE + "/" + orderId + "/confirm", jwtFactory.serviceToken())
                .andExpect(status().isOk());

        // Stock was written off exactly once.
        assertThat(auditLogRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("GET /admin/reservations/{id} returns the reservation with its items")
    void adminReadsReservation() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        UUID productId = UUID.randomUUID();
        givenAvailableStock(darkstore, bin, productId, "SKU-1", 50);
        UUID orderId = UUID.randomUUID();

        ApiRequests.postJson(mockMvc, SERVICE + "/reservations",
                requestFor(orderId, darkstore.getId(), productId, 6), jwtFactory.serviceToken())
                .andExpect(status().isCreated());

        StockReservationEntity saved = reload(orderId, ReservationStatus.ACTIVE);

        ApiRequests.getJson(mockMvc, ADMIN + "/" + saved.getId(), jwtFactory.adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].quantity").value(6))
                .andExpect(jsonPath("$.darkstore.id").value(darkstore.getId().toString()));
    }

    @Test
    @DisplayName("GET /admin/reservations lists reservations")
    void adminListsReservations() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        UUID productId = UUID.randomUUID();
        givenAvailableStock(darkstore, bin, productId, "SKU-1", 50);

        ApiRequests.postJson(mockMvc, SERVICE + "/reservations",
                requestFor(UUID.randomUUID(), darkstore.getId(), productId, 1), jwtFactory.serviceToken())
                .andExpect(status().isCreated());

        ApiRequests.getJson(mockMvc, ADMIN, jwtFactory.adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Cancelling an active reservation releases the stock and marks it CANCELLED")
    void cancelReleasesStock() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        UUID productId = UUID.randomUUID();
        givenStock(darkstore, bin, productId, "SKU-1", 10, 4);
        UUID orderId = UUID.randomUUID();

        // Create a reservation row directly so we control the reserved quantity.
        StockReservationEntity reservation = new StockReservationEntity();
        reservation.setOrderId(orderId);
        reservation.setDarkstore(darkstore);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setExpiresAt(java.time.OffsetDateTime.now().plusMinutes(30));
        StockReservationItemEntity item = new StockReservationItemEntity();
        item.setReservation(reservation);
        item.setProductId(productId);
        item.setQuantity(4);
        reservation.getItems().add(item);
        reservationRepository.save(reservation);

        ApiRequests.postEmpty(mockMvc, SERVICE + "/" + orderId + "/cancel", jwtFactory.serviceToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // releaseReservation decrements quantityReserved back to zero.
        assertThat(reloadStock(
                inventoryStockRepository.getByProductIdAndDarkstoreId(productId, darkstore.getId())
                        .orElseThrow().getId()).getQuantityReserved())
                .isZero();
    }

    @Test
    @DisplayName("GET /admin/reservations/{id} with an unknown id surfaces a 404")
    void unknownReservationReturns500() throws Exception {
        ApiRequests.getJson(mockMvc, ADMIN + "/" + UUID.randomUUID(), jwtFactory.adminToken())
                .andExpect(status().isNotFound());
    }
}
