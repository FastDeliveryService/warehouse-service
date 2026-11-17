package com.fds.warehouseservice.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.interfaces.RSAPublicKey;

/**
 * Replaces discovery/JWKS-over-HTTP with an in-process key so tests never need a running
 * Keycloak. The signature is still verified against a real RSA public key, so a tampered or
 * unsigned token is rejected exactly as it would be in production.
 *
 * <p>This only supplies the {@link JwtDecoder}. The authorization rules themselves come from
 * {@code FdsSecurityAutoConfiguration} and are deliberately left untouched, so these tests
 * validate the real production policy rather than a copy of it.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestSecurityConfiguration {

    /** Ordered so it wins over the auto-configured decoder that calls the issuer over HTTP. */
    @Bean
    @Order(-100)
    JwtDecoder testJwtDecoder(TestJwtFactory jwtFactory) {
        RSAKey rsaKey = (RSAKey) jwtFactory.publicJwks().getKeys().getFirst();

        RSAPublicKey publicKey;
        try {
            publicKey = rsaKey.toRSAPublicKey();
        } catch (JOSEException e) {
            throw new IllegalStateException("Test RSA key cannot be converted to a public key", e);
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        // Issuer validation stays enabled; the factory stamps the matching iss claim.
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtFactory.issuer()));
        return decoder;
    }
}
