package com.fds.warehouseservice.darkstores;

import com.fds.warehouseservice.dto.storagebin.StorageBinRequest;
import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.entity.darkstores.StorageBinEntity;
import com.fds.warehouseservice.entity.enums.StorageBinZone;
import com.fds.warehouseservice.support.ApiRequests;
import com.fds.warehouseservice.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code /api/v1/admin/darkstores/{darkstoreId}/bins}.
 *
 * <p>Focuses on the darkstore/binary scoping rules, the enum string conversion performed by
 * {@code StorageBinMapper}, and soft-delete semantics.
 */
class StorageBinControllerIT extends IntegrationTestBase {

    private static final String BINS = "/api/v1/admin/darkstores/%s/bins";

    private String binsFor(UUID darkstoreId) {
        return String.format(BINS, darkstoreId);
    }

    private StorageBinRequest validRequest() {
        return new StorageBinRequest("BIN-100", "AMBIENT", "A3", "S2");
    }

    @Test
    @DisplayName("POST creates a bin and converts a lowercase zone into the enum")
    void createsBinCaseInsensitively() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();

        ApiRequests.postJson(mockMvc, binsFor(darkstore.getId()),
                        new StorageBinRequest("BIN-1", "chilled", "A1", "S1"),
                        jwtFactory.adminToken())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.binCode").value("BIN-1"))
                .andExpect(jsonPath("$.zone").value("CHILLED"))
                .andExpect(jsonPath("$.darkstoreId").value(darkstore.getId().toString()));

        assertThat(storageBinRepository.readAllByDarkstoreIdAndActiveTrue(darkstore.getId(),
                PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("POST rejects an unknown zone value with 400")
    void rejectsUnknownZone() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();

        ApiRequests.postJson(mockMvc, binsFor(darkstore.getId()),
                        new StorageBinRequest("BIN-X", "TROPICAL", "A1", "S1"),
                        jwtFactory.adminToken())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST against an unknown darkstore fails rather than creating an orphan bin")
    void rejectsUnknownDarkstore() throws Exception {
        ApiRequests.postJson(mockMvc, binsFor(UUID.randomUUID()), validRequest(), jwtFactory.adminToken())
                .andExpect(status().isNotFound());

        assertThat(storageBinRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("GET /{id} returns the bin only within its own darkstore")
    void getBinIsScopedToDarkstore() throws Exception {
        DarkstoreEntity own = givenDarkstore("DS-OWN", "Own");
        DarkstoreEntity other = givenDarkstore("DS-OTHER", "Other");
        StorageBinEntity bin = givenStorageBin(own, "BIN-A", StorageBinZone.FROZEN, "A1", "S1");

        // Correct darkstore: found.
        ApiRequests.getJson(mockMvc, binsFor(own.getId()) + "/" + bin.getId(), jwtFactory.adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.binCode").value("BIN-A"))
                .andExpect(jsonPath("$.zone").value("FROZEN"));

        // Wrong darkstore: the scoped lookup misses and no foreign bin is leaked.
        ApiRequests.getJson(mockMvc, binsFor(other.getId()) + "/" + bin.getId(), jwtFactory.adminToken())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT updates bin attributes")
    void updatesBin() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);

        ApiRequests.putJson(mockMvc, binsFor(darkstore.getId()) + "/" + bin.getId(),
                        new StorageBinRequest("BIN-NEW", "FROZEN", "B9", "S9"),
                        jwtFactory.adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.binCode").value("BIN-NEW"))
                .andExpect(jsonPath("$.zone").value("FROZEN"))
                .andExpect(jsonPath("$.aisle").value("B9"))
                .andExpect(jsonPath("$.shelf").value("S9"));

        StorageBinEntity reloaded = storageBinRepository.findById(bin.getId()).orElseThrow();
        assertThat(reloaded.getZone()).isEqualTo(StorageBinZone.FROZEN);
        assertThat(reloaded.getBinCode()).isEqualTo("BIN-NEW");
    }

    @Test
    @DisplayName("DELETE soft-deletes, hiding the bin from subsequent reads")
    void softDeletesBin() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);

        ApiRequests.deleteJson(mockMvc, binsFor(darkstore.getId()) + "/" + bin.getId(), jwtFactory.adminToken())
                .andExpect(status().isNoContent());

        // Row is retained but inactive.
        assertThat(storageBinRepository.findById(bin.getId()).orElseThrow().getActive()).isFalse();

        // And the scoped read that requires active=true can no longer see it.
        ApiRequests.getJson(mockMvc, binsFor(darkstore.getId()) + "/" + bin.getId(), jwtFactory.adminToken())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET lists only active bins of the requested darkstore")
    void listsOnlyActiveBinsOfDarkstore() throws Exception {
        DarkstoreEntity target = givenDarkstore("DS-T", "Target");
        DarkstoreEntity other = givenDarkstore("DS-O", "Other");

        givenStorageBin(target, "BIN-KEEP", StorageBinZone.AMBIENT, "A1", "S1");
        StorageBinEntity inactive = givenStorageBin(target, "BIN-DEAD", StorageBinZone.AMBIENT, "A2", "S2");
        inactive.setActive(false);
        storageBinRepository.save(inactive);
        givenStorageBin(other, "BIN-FOREIGN", StorageBinZone.AMBIENT, "A3", "S3");

        String body = ApiRequests.getJson(mockMvc, binsFor(target.getId()), jwtFactory.adminToken())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .contains("BIN-KEEP")
                .doesNotContain("BIN-DEAD")     // inactive
                .doesNotContain("BIN-FOREIGN"); // different darkstore
    }

    @Test
    @DisplayName("POST rejects a blank bin code")
    void rejectsBlankBinCode() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();

        ApiRequests.postJson(mockMvc, binsFor(darkstore.getId()),
                        new StorageBinRequest("", "AMBIENT", "A1", "S1"),
                        jwtFactory.adminToken())
                .andExpect(status().isBadRequest());
    }
}
