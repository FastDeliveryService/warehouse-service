package com.fds.warehouseservice.security;

import com.fds.warehouseservice.dto.darkstores.DarkstoreRequest;
import com.fds.warehouseservice.dto.reservations.ReservationRequest;
import com.fds.warehouseservice.dto.stocks.StockRequest;
import com.fds.warehouseservice.entity.darkstores.DarkstoreEntity;
import com.fds.warehouseservice.entity.darkstores.StorageBinEntity;
import com.fds.warehouseservice.support.ApiRequests;
import com.fds.warehouseservice.support.IntegrationTestBase;
import com.fds.warehouseservice.support.TestJwtFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the authorization policy enforced by
 * {@code com.fds:security-spring-boot-starter}.
 *
 * <p>These intentionally do not import any test security configuration beyond the decoder, so
 * the real {@code FdsSecurityAutoConfiguration} rules are what is under test:
 * {@code /api/v1/admin/**} requires {@code ADMIN}, {@code /api/v1/service/**} requires
 * {@code SERVICE}, {@code /api/v1/public/**} is open, and everything else needs authentication.
 *
 * <p>Also documents the gap called out in AGENTS.md: {@code /api/v1/worker/**} is not covered
 * by any role rule, so any authenticated caller can reach it.
 */
class SecurityAuthorizationIT extends IntegrationTestBase {

    private static final String ADMIN_PATH = "/api/v1/admin/darkstores";
    private static final String SERVICE_PATH = "/api/v1/service/reservations";
    private static final String WORKER_PATH = "/api/v1/worker/tasks";

    private DarkstoreRequest anyDarkstore() {
        return new DarkstoreRequest("DS-SEC", "Sec", "Addr",
                new BigDecimal("50.0"), new BigDecimal("14.0"));
    }

    @Test
    @DisplayName("An unauthenticated request to an admin endpoint is rejected as 401")
    void anonymousAdminIsUnauthorized() throws Exception {
        mockMvc.perform(get(ADMIN_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("An unauthenticated request to a service endpoint is rejected as 401")
    void anonymousServiceIsUnauthorized() throws Exception {
        mockMvc.perform(get(SERVICE_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A syntactically invalid token is rejected")
    void malformedTokenRejected() throws Exception {
        mockMvc.perform(get(ADMIN_PATH).header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A token signed by an untrusted key is rejected")
    void tokenSignedByWrongKeyRejected() throws Exception {
        // A second factory holds a different RSA key, so signature verification must fail.
        TestJwtFactory rogue = new TestJwtFactory();
        String forged = rogue.adminToken();

        mockMvc.perform(get(ADMIN_PATH).header("Authorization", TestJwtFactory.bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("An ADMIN token can reach admin endpoints")
    void adminTokenReachesAdminEndpoint() throws Exception {
        ApiRequests.postJson(mockMvc, ADMIN_PATH, anyDarkstore(), jwtFactory.adminToken())
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("A SERVICE token is forbidden from admin endpoints")
    void serviceTokenForbiddenOnAdminEndpoint() throws Exception {
        ApiRequests.postJson(mockMvc, ADMIN_PATH, anyDarkstore(), jwtFactory.serviceToken())
                .andExpect(status().isForbidden());

        assertThat(darkstoreRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("An ADMIN token is forbidden from service endpoints")
    void adminTokenForbiddenOnServiceEndpoint() throws Exception {
        ReservationRequest request = new ReservationRequest(
                UUID.randomUUID(), UUID.randomUUID(), List.of());

        ApiRequests.postJson(mockMvc, SERVICE_PATH, request, jwtFactory.adminToken())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An authenticated token with no FDS roles is forbidden from role-guarded paths")
    void authenticatedWithoutRolesIsForbidden() throws Exception {
        ApiRequests.getJson(mockMvc, ADMIN_PATH, jwtFactory.userToken())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A SERVICE token can reach service endpoints")
    void serviceTokenReachesServiceEndpoint() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);
        UUID productId = UUID.randomUUID();
        givenAvailableStock(darkstore, bin, productId, "SKU-1", 10);

        ApiRequests.postJson(mockMvc, SERVICE_PATH,
                        new com.fds.warehouseservice.dto.reservations.ReservationRequest(
                                UUID.randomUUID(), darkstore.getId(),
                                java.util.List.of(new com.fds.warehouseservice.dto.reservations.ReservationItemRequest(
                                        productId, 1))),
                        jwtFactory.serviceToken())
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET on admin path with a SERVICE token is forbidden regardless of HTTP method")
    void serviceTokenForbiddenOnAdminRead() throws Exception {
        ApiRequests.getJson(mockMvc, ADMIN_PATH, jwtFactory.serviceToken())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("/api/v1/worker/** is only authenticated, not role-restricted (documented gap)")
    void workerPathIsNotRoleRestricted() throws Exception {
        ApiRequests.patchEmpty(mockMvc, WORKER_PATH + "/" + UUID.randomUUID() + "/complete",
                        jwtFactory.userToken())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An unauthenticated call to a worker endpoint is still rejected as 401")
    void anonymousWorkerIsUnauthorized() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch(WORKER_PATH + "/" + UUID.randomUUID() + "/complete"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("An admin request with a valid role but unknown darkstore still passes authorization")
    void authorizationPrecedesBusinessValidation() throws Exception {
        DarkstoreEntity darkstore = givenDarkstore();
        StorageBinEntity bin = givenStorageBin(darkstore);

        ApiRequests.postJson(mockMvc, "/api/v1/admin/inventory/inbound",
                        new StockRequest(UUID.randomUUID(), "SKU-SEC", 1, UUID.randomUUID(), bin.getId()),
                        jwtFactory.adminToken())
                .andExpect(status().isNotFound());
    }
}
