package com.fds.warehouseservice.stocks;

import com.fds.warehouseservice.dto.stocks.AdjustmentStockRequest;
import com.fds.warehouseservice.dto.stocks.StockRequest;
import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.entity.darkstores.StorageBinEntity;
import com.fds.warehouseservice.entity.enums.StorageBinZone;
import com.fds.warehouseservice.entity.stocks.InventoryStocksEntity;
import com.fds.warehouseservice.entity.stocks.StockAuditLogEntity;
import com.fds.warehouseservice.repository.stocks.StockAuditLogRepository;
import com.fds.warehouseservice.support.ApiRequests;
import com.fds.warehouseservice.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static com.fds.warehouseservice.support.TestJwtFactory.SYSTEM_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code /api/v1/admin/inventory}.
 *
 * <p>Verifies the inbound/adjust flows together with the two invariants that matter most:
 * {@code quantityAvailable} is always derived rather than stored, and every mutation leaves
 * exactly one audit row with the correct resulting quantity.
 */
class InventoryControllerIT extends IntegrationTestBase {

    private static final String BASE = "/api/v1/admin/inventory";

    @Autowired
    private StockAuditLogRepository auditLogRepository;

    @Test
    @DisplayName("POST /inbound creates a stock row and returns 201")
    void inboundCreatesStock() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        UUID productId = UUID.randomUUID();

        ApiRequests.postJson(mockMvc, BASE + "/inbound",
                        new StockRequest(productId, "SKU-1", 25, darkstore.getId(), bin.getId()),
                        jwtFactory.adminToken())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.sku").value("SKU-1"))
                .andExpect(jsonPath("$.quantity").value(25));

        InventoryStocksEntity stock = inventoryStockRepository
                .getByProductIdAndDarkstoreId(productId, darkstore.getId()).orElseThrow();

        assertThat(stock.getQuantityOnHand()).isEqualTo(25);
        assertThat(stock.getQuantityReserved()).isZero();
    }

    @Test
    @DisplayName("POST /inbound for an existing product accumulates quantity on the same row")
    void inboundAccumulates() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        UUID productId = UUID.randomUUID();

        ApiRequests.postJson(mockMvc, BASE + "/inbound",
                new StockRequest(productId, "SKU-1", 10, darkstore.getId(), bin.getId()),
                jwtFactory.adminToken()).andExpect(status().isCreated());

        ApiRequests.postJson(mockMvc, BASE + "/inbound",
                        new StockRequest(productId, "SKU-1", 15, darkstore.getId(), bin.getId()),
                        jwtFactory.adminToken())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(25));

        // The unique constraint on (darkstore_id, product_id) means one row, not two.
        assertThat(inventoryStockRepository.findAll()).hasSize(1);
        assertThat(inventoryStockRepository
                .getByProductIdAndDarkstoreId(productId, darkstore.getId()).orElseThrow()
                .getQuantityOnHand()).isEqualTo(25);
    }

    @Test
    @DisplayName("Reported quantity is on-hand minus reserved, never the raw on-hand value")
    void quantityIsDerived() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        InventoryStocksEntity stock = givenStock(darkstore, bin, UUID.randomUUID(), "SKU-1", 100, 30);

        ApiRequests.getJson(mockMvc, BASE + "/" + stock.getId(), jwtFactory.adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(70));
    }

    @Test
    @DisplayName("POST /adjust applies a negative change and records an ADJUSTMENT audit row")
    void adjustDecreasesQuantityAndAudits() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        InventoryStocksEntity stock = givenAvailableStock(darkstore, bin, UUID.randomUUID(), "SKU-1", 50);

        ApiRequests.postJson(mockMvc, BASE + "/adjust",
                        new AdjustmentStockRequest(stock.getId(), -8, "DAMAGE", "Crushed boxes"),
                        jwtFactory.adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(42));

        assertThat(reloadStock(stock.getId()).getQuantityOnHand()).isEqualTo(42);
        assertThat(auditLogRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("POST /adjust accepts a lowercase reason code")
    void adjustAcceptsCaseInsensitiveReason() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        InventoryStocksEntity stock = givenAvailableStock(darkstore, bin, UUID.randomUUID(), "SKU-1", 10);

        ApiRequests.postJson(mockMvc, BASE + "/adjust",
                        new AdjustmentStockRequest(stock.getId(), 5, "relocation", "Moved shelf"),
                        jwtFactory.adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(15));
    }

    @Test
    @DisplayName("POST /adjust rejects an unknown reason code with 400")
    void adjustRejectsUnknownReason() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        InventoryStocksEntity stock = givenAvailableStock(darkstore, bin, UUID.randomUUID(), "SKU-1", 10);

        ApiRequests.postJson(mockMvc, BASE + "/adjust",
                        new AdjustmentStockRequest(stock.getId(), -1, "ALIENS", "note"),
                        jwtFactory.adminToken())
                .andExpect(status().isBadRequest());

        // Rejected before any mutation.
        assertThat(reloadStock(stock.getId()).getQuantityOnHand()).isEqualTo(10);
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("POST /inbound rejects a non-positive quantity")
    void inboundRejectsNonPositiveQuantity() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);

        ApiRequests.postJson(mockMvc, BASE + "/inbound",
                        new StockRequest(UUID.randomUUID(), "SKU-1", 0, darkstore.getId(), bin.getId()),
                        jwtFactory.adminToken())
                .andExpect(status().isBadRequest());

        assertThat(inventoryStockRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("POST /inbound rejects a bin that belongs to a different darkstore")
    void inboundRejectsForeignBin() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore("DS-A", "A");
        DarkstoreEntity other = givenDarkstore("DS-B", "B");
        StorageBinEntity foreignBin = givenStorageBin(other, "BIN-B", com.fds.warehouseservice.entity.enums.StorageBinZone.AMBIENT, "A1", "S1");

        ApiRequests.postJson(mockMvc, BASE + "/inbound",
                        new StockRequest(UUID.randomUUID(), "SKU-1", 5, darkstore.getId(), foreignBin.getId()),
                        jwtFactory.adminToken())
                .andExpect(status().isNotFound());

        assertThat(inventoryStockRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Inbound writes an audit row attributed to the system user")
    void inboundWritesSystemAuditRow() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        UUID productId = UUID.randomUUID();

        ApiRequests.postJson(mockMvc, BASE + "/inbound",
                        new StockRequest(productId, "SKU-1", 7, darkstore.getId(), bin.getId()),
                        jwtFactory.adminToken())
                .andExpect(status().isCreated());

        StockAuditLogEntity log = auditLogRepository.findAll().getFirst();
        assertThat(log.getOperationType().name()).isEqualTo("INBOUND");
        assertThat(log.getQuantityChange()).isEqualTo(7);
        assertThat(log.getResultingQuantity()).isEqualTo(7);
        assertThat(log.getProductId()).isEqualTo(productId);
        // Inbound is a system operation, so the constant system id is recorded, not the caller.
        assertThat(log.getPerformedBy()).isEqualTo(SYSTEM_USER_ID);
    }

    @Test
    @DisplayName("LIST returns a page of stocks")
    void listsStocks() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        givenAvailableStock(darkstore, bin, UUID.randomUUID(), "SKU-A", 5);

        String body = ApiRequests.getJson(mockMvc, BASE, jwtFactory.adminToken())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("SKU-A");
    }

    @Test
    @DisplayName("POST /inbound against an unknown darkstore is rejected with no side effects")
    void inboundUnknownDarkstore() throws Exception {
        ApiRequests.postJson(mockMvc, BASE + "/inbound",
                        new StockRequest(UUID.randomUUID(), "SKU-1", 5, UUID.randomUUID(), UUID.randomUUID()),
                        jwtFactory.adminToken())
                .andExpect(status().isNotFound());

        assertThat(inventoryStockRepository.findAll()).isEmpty();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("The database rejects negative on-hand quantities outright")
    void databaseRejectsNegativeOnHand() {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        UUID productId = UUID.randomUUID();

        InventoryStocksEntity stock = givenAvailableStock(darkstore, bin, productId, "SKU-1", 1);

        // The chk_quantity_on_hand constraint is the last line of defence behind the service.
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE inventory_stocks SET quantity_on_hand = -1 WHERE id = ?", stock.getId()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("GET /{id} for an unknown stock surfaces a 404")
    void unknownStockReturns500() throws Exception {
        // NoSuchElementException is unmapped by AppExceptionHandler today; pin the behaviour.
        ApiRequests.getJson(mockMvc, BASE + "/" + UUID.randomUUID(), jwtFactory.adminToken())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("The stock repository rejects a duplicate (darkstore, product) pair")
    void uniqueConstraintOnDarkstoreAndProduct() {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        UUID productId = UUID.randomUUID();

        givenAvailableStock(darkstore, bin, productId, "SKU-1", 5);
        StorageBinEntity secondBin = givenStorageBin(darkstore, "BIN-02", StorageBinZone.AMBIENT, "A2", "S2");

        assertThatThrownBy(() -> givenAvailableStock(darkstore, secondBin, productId, "SKU-1", 5))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
