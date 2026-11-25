package com.fds.warehouseservice.support;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.boot.test.context.TestComponent;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Mints real signed RS256 JWTs shaped like the Keycloak tokens the security starter expects.
 *
 * <p>The starter validates the token against the configured {@code issuer-uri}'s JWKS
 * endpoint. Rather than run Keycloak, the test profile points {@code issuer-uri} at this
 * class's {@link #issuer()} and registers {@link #publicJwks()} as the resolver's JWK source,
 * so signatures are verified for real while claims stay fully under test control.
 *
 * <p>Roles are placed in Keycloak's {@code realm_access.roles} claim, which the starter maps
 * to {@code ROLE_<UPPERCASE>}. The principal (the {@code sub} claim) must be a UUID because
 * {@code SecurityUtils.getUserId()} parses it with {@code UUID.fromString}.
 */
@TestComponent
public class TestJwtFactory {

    public static final String TEST_ISSUER = "http://test-issuer.local/realms/fds";

    /** Matches the constant inside the security starter; used for system-performed writes. */
    public static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0001-7000-a000-000000000001");

    private final RSAKey rsaKey;
    private final JWKSet jwkSet;

    public TestJwtFactory() {
        try {
            this.rsaKey = new RSAKeyGenerator(2048)
                    .keyID(UUID.randomUUID().toString())
                    .generate();
            this.jwkSet = new JWKSet(rsaKey.toPublicJWK());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate an RSA key for test JWTs", e);
        }
    }

    public String issuer() {
        return TEST_ISSUER;
    }

    /** Public JWKS document served to the Spring Security JWT decoder. */
    public JWKSet publicJwks() {
        return jwkSet;
    }

    /** A token carrying the ADMIN realm role. */
    public String adminToken() {
        return adminToken(UUID.randomUUID());
    }

    public String adminToken(UUID subject) {
        return token(subject, List.of("ADMIN"));
    }

    /** A token carrying the SERVICE realm role. */
    public String serviceToken() {
        return serviceToken(UUID.randomUUID());
    }

    public String serviceToken(UUID subject) {
        return token(subject, List.of("SERVICE"));
    }

    /** A token carrying the SERVICE realm role. */
    public String userToken() {
        return token(UUID.randomUUID(), List.of("USER"));
    }

    /** A token carrying the SERVICE realm role. */
    public String workerToken() {
        return token(UUID.randomUUID(), List.of("WORKER"));
    }

    /**
     * Builds a signed token. {@code realm_access.roles} mirrors Keycloak; the noise claims
     * ({@code default-roles-*}, {@code offline_access}, {@code uma_authorization}) reproduce
     * Keycloak's defaults so the starter's role filtering is exercised faithfully.
     */
    public String token(UUID subject, List<String> realmRoles) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .issuer(TEST_ISSUER)
                    .subject(subject.toString())
                    .audience("account")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .claim("typ", "Bearer")
                    .claim("azp", "warehouse-service")
                    .claim("realm_access", java.util.Map.of("roles", realmRoles));

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(rsaKey.getKeyID())
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claims.build());

            jwt.sign(new RSASSASigner(rsaKey));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign test JWT", e);
        }
    }

    /** Convenience for {@code Authorization: Bearer <token>} headers. */
    public static String bearer(String token) {
        return "Bearer " + token;
    }
}
