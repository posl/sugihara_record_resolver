package org.apereo.cas.oidc.token;

import org.apereo.cas.oidc.issuer.OidcIssuerService;
import org.apereo.cas.oidc.jwks.OidcJsonWebKeyCacheKey;
import org.apereo.cas.oidc.jwks.OidcJsonWebKeyUsage;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.support.oauth.web.response.accesstoken.response.OAuth20RegisteredServiceJwtAccessTokenCipherExecutor;
import org.apereo.cas.token.cipher.JwtTicketCipherExecutor;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.EncodingUtils;

import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Unchecked;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.PublicJsonWebKey;

import java.io.Serializable;
import java.security.Key;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This is {@link OidcRegisteredServiceJwtAccessTokenCipherExecutor}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public class OidcRegisteredServiceJwtAccessTokenCipherExecutor extends OAuth20RegisteredServiceJwtAccessTokenCipherExecutor {
    /**
     * The default keystore for OIDC tokens.
     */
    protected final LoadingCache<OidcJsonWebKeyCacheKey, Optional<JsonWebKeySet>> defaultJsonWebKeystoreCache;

    /**
     * The service keystore for OIDC tokens.
     */
    protected final LoadingCache<OidcJsonWebKeyCacheKey, Optional<JsonWebKeySet>> serviceJsonWebKeystoreCache;

    /**
     * OIDC issuer.
     */
    protected final OidcIssuerService oidcIssuerService;

    private static PublicJsonWebKey toJsonWebKey(final String key) throws Exception {
        val details = EncodingUtils.parseJsonWebKey(key);
        if (details.containsKey(JsonWebKeySet.JWK_SET_MEMBER_NAME)) {
            return (PublicJsonWebKey) new JsonWebKeySet(key).getJsonWebKeys().get(0);
        }
        return (PublicJsonWebKey) EncodingUtils.newJsonWebKey(key);
    }

    @Override
    public Optional<String> getSigningKey(final RegisteredService registeredService) {
        if (!isSigningEnabledForRegisteredService(registeredService)) {
            return Optional.empty();
        }
        val result = super.getSigningKey(registeredService);
        if (result.isPresent()) {
            return result;
        }
        val oidcRegisteredService = OidcRegisteredService.class.cast(registeredService);
        val issuer = oidcIssuerService.determineIssuer(Optional.of(oidcRegisteredService));
        LOGGER.trace("Using issuer [{}] to determine JWKS from default keystore cache", issuer);
        var jwks = Objects.requireNonNull(serviceJsonWebKeystoreCache.get(
            new OidcJsonWebKeyCacheKey(oidcRegisteredService, OidcJsonWebKeyUsage.SIGNING)));
        if (jwks.isPresent()) {
            val jsonWebKey = jwks.get();
            LOGGER.debug("Found JSON web key to sign the token: [{}]", jsonWebKey);
            val keys = jsonWebKey.getJsonWebKeys().stream()
                .filter(key -> key.getKey() != null).collect(Collectors.toList());
            return Optional.of(new JsonWebKeySet(keys).toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE));
        }
        jwks = Objects.requireNonNull(defaultJsonWebKeystoreCache.get(
            new OidcJsonWebKeyCacheKey(issuer, OidcJsonWebKeyUsage.SIGNING)));
        if (jwks.isEmpty()) {
            LOGGER.warn("No signing key could be found for issuer " + issuer);
            return Optional.empty();
        }
        return Optional.of(jwks.get().toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE));
    }

    @Override
    public Optional<String> getEncryptionKey(final RegisteredService registeredService) {
        if (!isEncryptionEnabledForRegisteredService(registeredService)) {
            return Optional.empty();
        }
        val svc = (OAuthRegisteredService) registeredService;
        val result = super.getEncryptionKey(registeredService);
        if (result.isPresent()) {
            return result;
        }

        if (svc instanceof OidcRegisteredService) {
            val jwks = Objects.requireNonNull(serviceJsonWebKeystoreCache.get(
                new OidcJsonWebKeyCacheKey(svc, OidcJsonWebKeyUsage.ENCRYPTION)));
            if (jwks.isEmpty()) {
                LOGGER.warn("Service " + svc.getServiceId()
                            + " with client id " + svc.getClientId()
                            + " is configured to encrypt tokens, yet no JSON web key is available");
                return Optional.empty();
            }
            val jsonWebKey = jwks.get();
            LOGGER.debug("Found JSON web key to encrypt the token: [{}]", jsonWebKey);

            val keys = jsonWebKey.getJsonWebKeys().stream()
                .filter(key -> key.getKey() != null).collect(Collectors.toList());
            if (keys.isEmpty()) {
                LOGGER.warn("No valid JSON web keys used to sign the token can be found");
                return Optional.empty();
            }
            return Optional.of(new JsonWebKeySet(keys).toJson());
        }
        return result;
    }

    @Override
    protected JwtTicketCipherExecutor createCipherExecutorInstance(
        final String encryptionKey,
        final String signingKey,
        final RegisteredService registeredService,
        final CipherOperationsStrategyType type) {

        val cipher = new InternalJwtAccessTokenCipherExecutor(encryptionKey, signingKey);
        Unchecked.consumer(c -> {
            if (EncodingUtils.isJsonWebKey(encryptionKey)) {
                val jsonWebKey = toJsonWebKey(encryptionKey);
                cipher.setEncryptionKey(jsonWebKey.getPublicKey());
                cipher.setEncryptionWebKey(jsonWebKey);
            }
            if (EncodingUtils.isJsonWebKey(signingKey)) {
                val jsonWebKey = toJsonWebKey(signingKey);

                /**
                 * Use the private key as the primary key to handle signing operations.
                 * The private key may also be used for validating signed objects.
                 * If the private key is not found, in the case where the keystore only contains a public
                 * key, then use the public key as the primary signing key, turning this cipher into one
                 * that can only verify signed objects, which would be useful when processing signed
                 * request objects that are sent by the RP, which has the only copy of the private key.
                 */
                cipher.setSigningKey(ObjectUtils.defaultIfNull(jsonWebKey.getPrivateKey(), jsonWebKey.getKey()));
                cipher.setSigningWebKey(jsonWebKey);
            }
        }).accept(cipher);

        if (EncodingUtils.isJsonWebKey(encryptionKey) || EncodingUtils.isJsonWebKey(signingKey)) {
            cipher.setEncryptionAlgorithm(KeyManagementAlgorithmIdentifiers.RSA_OAEP_256);
        }
        cipher.setCustomHeaders(CollectionUtils.wrap(CUSTOM_HEADER_REGISTERED_SERVICE_ID, registeredService.getId()));
        cipher.setStrategyType(type);
        return cipher;
    }

    @Setter
    @Getter
    @SuppressWarnings("UnusedMethod")
    private class InternalJwtAccessTokenCipherExecutor extends JwtTicketCipherExecutor {
        private JsonWebKey signingWebKey;

        private JsonWebKey encryptionWebKey;

        InternalJwtAccessTokenCipherExecutor(final String encryptionKey, final String signingKey) {
            super(encryptionKey, signingKey, StringUtils.isNotBlank(encryptionKey),
                StringUtils.isNotBlank(signingKey), 0, 0);
        }

        @Override
        protected byte[] sign(final byte[] value, final Key signingKey) {
            return Optional.ofNullable(this.signingWebKey)
                .map(key -> {
                    val kid = key.getKeyId();
                    if (StringUtils.isNotBlank(kid)) {
                        getCustomHeaders().put(JsonWebKey.KEY_ID_PARAMETER, kid);
                    }
                    val alg = StringUtils.defaultIfBlank(key.getAlgorithm(),
                        getSigningAlgorithmFor(key.getKey()));
                    getCustomHeaders().put(JsonWebKey.ALGORITHM_PARAMETER, alg);
                    return super.signWith(value, alg, signingKey);
                })
                .orElseGet(() -> super.sign(value, signingKey));
        }

        @Override
        protected String decode(final Serializable value, final Object[] parameters,
                                final Key encKey, final Key signingKey) {
            if (parameters.length > 0) {
                val registeredService = (RegisteredService) parameters[0];
                val decryptionKey = getEncryptionKeyForDecryption(registeredService);
                return super.decode(value, parameters, decryptionKey, signingKey);
            }
            return super.decode(value, parameters, encKey, signingKey);
        }
    }

    private Key getEncryptionKeyForDecryption(final RegisteredService registeredService) {
        val svc = (OAuthRegisteredService) registeredService;
        if (svc instanceof OidcRegisteredService) {
            val jwks = Objects.requireNonNull(this.serviceJsonWebKeystoreCache.get(
                new OidcJsonWebKeyCacheKey(svc, OidcJsonWebKeyUsage.ENCRYPTION)));
            if (jwks.isEmpty()) {
                LOGGER.warn("Service " + svc.getServiceId()
                            + " with client id " + svc.getClientId()
                            + " is configured to encrypt tokens, yet no JSON web key is available");
                return null;
            }
            val jsonWebKey = (PublicJsonWebKey) jwks.get().getJsonWebKeys().get(0);
            LOGGER.debug("Found JSON web key to encrypt the token: [{}]", jsonWebKey);
            if (jsonWebKey.getPrivateKey() == null) {
                LOGGER.info("JSON web key used to encrypt the token has no associated private key, "
                            + "when operating on service [{}] with client id [{}]. Operations that deal "
                            + "with JWT encryption/decryption may not be functional, until a private "
                            + "key can be loaded for JSON web key [{}]",
                    svc.getServiceId(), svc.getClientId(), jsonWebKey.getKeyId());
                return null;
            }
            return jsonWebKey.getPrivateKey();
        }
        return null;
    }
}
