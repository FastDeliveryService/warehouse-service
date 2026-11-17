package com.fds.warehouseservice.support;

import com.fds.warehouseservice.TestcontainersConfiguration;
import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.entity.darkstores.StorageBinEntity;
import com.fds.warehouseservice.entity.enums.StorageBinZone;
import com.fds.warehouseservice.entity.stocks.InventoryStocksEntity;
import com.fds.warehouseservice.repository.darkstores.DarkstoreRepository;
import com.fds.warehouseservice.repository.darkstores.StorageBinRepository;
import com.fds.warehouseservice.repository.stocks.InventoryStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Base class for full-stack integration tests.
 *
 * <p>Boots the whole application against a real PostgreSQL instance provided by
 * {@link TestcontainersConfiguration}, so Liquibase migrations, JPA mappings, Hibernate
 * validation ({@code ddl-auto: validate}) and the security filter chain are all exercised
 * for real. HTTP calls are issued through {@link MockMvc} against the real
 * {@code FilterChainProxy}, with genuine signed JWTs supplied by {@link TestJwtFactory}.
 *
 * <p>The schema is truncated before each test so tests are order-independent and can run
 * without interfering. Truncation is done through a single {@code TRUNCATE ... CASCADE}
 * statement rather than per-test rollback, because the code under test opens its own
 * transaction boundaries.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class, TestJwtFactory.class})
public abstract class IntegrationTestBase {

    /** Every table created by the Liquibase changelogs. */
    private static final List<String> TABLES = List.of(
            "picking_task_items",
            "picking_tasks",
            "stock_reservation_items",
            "stock_reservations",
            "stock_audit_logs",
            "inventory_stocks",
            "storage_bins",
            "darkstores"
    );

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestJwtFactory jwtFactory;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected DarkstoreRepository darkstoreRepository;

    @Autowired
    protected StorageBinRepository storageBinRepository;

    @Autowired
    protected InventoryStockRepository inventoryStockRepository;

    @BeforeEach
    void resetDatabase() {
        // A single statement keeps this atomic and sidesteps FK ordering problems.
        jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", TABLES) + " CASCADE");
    }

    // ------------------------------------------------------------------
    // Fixture builders
    // ------------------------------------------------------------------

    /** Persists an active darkstore with sensible defaults. */
    protected DarkstoreEntity givenDarkstore() {
        return givenDarkstore("DS-TEST", "Test Darkstore");
    }

    protected DarkstoreEntity givenDarkstore(String code, String name) {
        DarkstoreEntity darkstore = new DarkstoreEntity();
        darkstore.setCode(code);
        darkstore.setName(name);
        darkstore.setAddress("Test Street 1");
        darkstore.setLatitude(new BigDecimal("52.22970000"));
        darkstore.setLongitude(new BigDecimal("21.01180000"));
        darkstore.setActive(true);
        return darkstoreRepository.save(darkstore);
    }

    /** Persists an active storage bin inside the given darkstore. */
    protected StorageBinEntity givenStorageBin(DarkstoreEntity darkstore) {
        return givenStorageBin(darkstore, "BIN-01", StorageBinZone.AMBIENT, "A1", "S1");
    }

    protected StorageBinEntity givenStorageBin(DarkstoreEntity darkstore,
                                               String binCode,
                                               StorageBinZone zone,
                                               String aisle,
                                               String shelf) {
        StorageBinEntity bin = new StorageBinEntity();
        bin.setDarkstore(darkstore);
        bin.setBinCode(binCode);
        bin.setZone(zone);
        bin.setAisle(aisle);
        bin.setShelf(shelf);
        bin.setActive(true);
        return storageBinRepository.save(bin);
    }

    /** Persists a stock row directly, bypassing the inbound endpoint. */
    protected InventoryStocksEntity givenStock(DarkstoreEntity darkstore,
                                               StorageBinEntity bin,
                                               UUID productId,
                                               String sku,
                                               int quantityOnHand,
                                               int quantityReserved) {
        InventoryStocksEntity stock = new InventoryStocksEntity();
        stock.setDarkstore(darkstore);
        stock.setStorageBin(bin);
        stock.setProductId(productId);
        stock.setSku(sku);
        stock.setQuantityOnHand(quantityOnHand);
        stock.setQuantityReserved(quantityReserved);
        return inventoryStockRepository.save(stock);
    }

    /** Convenience: an inbound product with all quantity available. */
    protected InventoryStocksEntity givenAvailableStock(DarkstoreEntity darkstore,
                                                        StorageBinEntity bin,
                                                        UUID productId,
                                                        String sku,
                                                        int quantity) {
        return givenStock(darkstore, bin, productId, sku, quantity, 0);
    }

    /** Re-reads a stock row in a fresh transaction, bypassing the first-level cache. */
    protected InventoryStocksEntity reloadStock(UUID stockId) {
        return inventoryStockRepository.findById(stockId).orElseThrow();
    }
}
