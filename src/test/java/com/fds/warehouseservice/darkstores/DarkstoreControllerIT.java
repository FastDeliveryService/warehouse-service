package com.fds.warehouseservice.darkstores;

import com.fds.warehouseservice.dto.darkstores.DarkstoreRequest;
import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.support.ApiRequests;
import com.fds.warehouseservice.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code /api/v1/admin/darkstores}.
 *
 * <p>These exercise the full stack: security filter chain, validation, MapStruct mapping,
 * Hibernate and the real PostgreSQL schema produced by Liquibase.
 */
class DarkstoreControllerIT extends IntegrationTestBase {

    private static final String BASE = "/api/v1/admin/darkstores";

    private DarkstoreRequest validRequest() {
        return new DarkstoreRequest(
                "DS-001",
                "Prague Center",
                "Wenceslas Square 1",
                new BigDecimal("50.08100000"),
                new BigDecimal("14.43000000"));
    }

    @Test
    @DisplayName("POST creates a darkstore and returns 201 with the generated id")
    void createsDarkstore() throws Exception {
        String token = jwtFactory.adminToken();

        String body = ApiRequests.postJson(mockMvc, BASE, validRequest(), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.code").value("DS-001"))
                .andExpect(jsonPath("$.name").value("Prague Center"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(objectMapper.readTree(body).get("id").asString());

        // The row is genuinely persisted, not just echoed back.
        assertThat(darkstoreRepository.findById(id)).isPresent();
    }

    @Test
    @DisplayName("POST rejects a blank code with 400 and a field-level message")
    void rejectsBlankCode() throws Exception {
        DarkstoreRequest invalid = new DarkstoreRequest(
                "", "Name", "Address",
                new BigDecimal("50.0"), new BigDecimal("14.0"));

        ApiRequests.postJson(mockMvc, BASE, invalid, jwtFactory.adminToken())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value(org.hamcrest.Matchers.containsString("code")));

        assertThat(darkstoreRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("POST rejects out-of-range latitude")
    void rejectsLatitudeOutOfRange() throws Exception {
        DarkstoreRequest invalid = new DarkstoreRequest(
                "DS-002", "Name", "Address",
                new BigDecimal("95.0"), new BigDecimal("14.0"));

        ApiRequests.postJson(mockMvc, BASE, invalid, jwtFactory.adminToken())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /{id} returns a previously created darkstore")
    void readsDarkstoreById() throws Exception {
        DarkstoreEntity saved = givenDarkstore("DS-READ", "Readable");

        ApiRequests.getJson(mockMvc, BASE + "/" + saved.getId(), jwtFactory.adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.code").value("DS-READ"))
                .andExpect(jsonPath("$.name").value("Readable"));
    }

    @Test
    @DisplayName("PUT /{id} updates the mutable fields in place")
    void updatesDarkstore() throws Exception {
        DarkstoreEntity saved = givenDarkstore("DS-OLD", "Old Name");

        DarkstoreRequest update = new DarkstoreRequest(
                "DS-NEW", "New Name", "New Address 9",
                new BigDecimal("51.10000000"), new BigDecimal("17.03000000"));

        ApiRequests.putJson(mockMvc, BASE + "/" + saved.getId(), update, jwtFactory.adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DS-NEW"))
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.address").value("New Address 9"));

        assertThat(darkstoreRepository.findById(saved.getId()).orElseThrow().getName())
                .isEqualTo("New Name");
    }

    @Test
    @DisplayName("DELETE /{id} soft-deletes: the row survives but is deactivated")
    void softDeletesDarkstore() throws Exception {
        DarkstoreEntity saved = givenDarkstore("DS-DEL", "To Delete");

        ApiRequests.deleteJson(mockMvc, BASE + "/" + saved.getId(), jwtFactory.adminToken())
                .andExpect(status().isNoContent());

        // Soft delete: still present in the table, flagged inactive.
        DarkstoreEntity afterDelete = darkstoreRepository.findById(saved.getId()).orElseThrow();
        assertThat(afterDelete.getActive()).isFalse();
    }

    @Test
    @DisplayName("GET lists only active darkstores")
    void listsOnlyActiveDarkstores() throws Exception {
        givenDarkstore("DS-ACTIVE", "Active");
        DarkstoreEntity inactive = givenDarkstore("DS-INACTIVE", "Inactive");
        inactive.setActive(false);
        darkstoreRepository.save(inactive);

        String body = ApiRequests.getJson(mockMvc, BASE, jwtFactory.adminToken())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("DS-ACTIVE").doesNotContain("DS-INACTIVE");
    }

    @Test
    @DisplayName("A soft-deleted darkstore is no longer retrievable through the API")
    void deletedDarkstoreDisappearsFromList() throws Exception {
        DarkstoreEntity saved = givenDarkstore("DS-GONE", "Gone");

        ApiRequests.deleteJson(mockMvc, BASE + "/" + saved.getId(), jwtFactory.adminToken())
                .andExpect(status().isNoContent());

        String body = ApiRequests.getJson(mockMvc, BASE, jwtFactory.adminToken())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("DS-GONE");
    }

    @Test
    @DisplayName("GET /{id} with an unknown id surfaces a 404 status code")
    void unknownDarkstoreIdReturns500() throws Exception {
        ApiRequests.getJson(mockMvc, BASE + "/" + UUID.randomUUID(), jwtFactory.adminToken())
                .andExpect(status().isNotFound());
    }
}
