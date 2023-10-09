package org.apereo.cas.oidc.token;

import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.authentication.principal.WebApplicationServiceFactory;
import org.apereo.cas.mock.MockTicketGrantingTicket;
import org.apereo.cas.oidc.AbstractOidcTests;
import org.apereo.cas.oidc.OidcConstants;
import org.apereo.cas.services.DefaultRegisteredServiceProperty;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredServiceProperty;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.OAuth20GrantTypes;
import org.apereo.cas.support.oauth.OAuth20ResponseTypes;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.support.oauth.util.OAuth20Utils;
import org.apereo.cas.support.oauth.web.response.accesstoken.OAuth20AccessTokenAtHashGenerator;
import org.apereo.cas.support.oauth.web.response.accesstoken.response.OAuth20JwtAccessTokenEncoder;
import org.apereo.cas.ticket.accesstoken.OAuth20AccessToken;
import org.apereo.cas.util.CollectionUtils;

import lombok.val;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.Pac4jConstants;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.apereo.cas.oidc.OidcConstants.StandardScopes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link OidcIdTokenGeneratorServiceTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@Tag("OIDC")
public class OidcIdTokenGeneratorServiceTests {

    private static final String OIDC_CLAIM_EMAIL = "email";

    private static final String OIDC_CLAIM_PHONE_NUMBER = "phone_number";

    private static final String OIDC_CLAIM_NAME = "name";

    private static final String OIDC_CLAIM_PREFERRED_USERNAME = "preferred_username";

    private static class MockOAuthRegisteredService extends OAuthRegisteredService {
        @Serial
        private static final long serialVersionUID = 8152953800891665827L;
    }

    @Nested
    @SuppressWarnings("ClassCanBeStatic")
    @TestPropertySource(properties = {
        "cas.authn.oauth.access-token.crypto.encryption-enabled=false",
        "cas.authn.oidc.id-token.include-id-token-claims=true"
    })
    public class IgnoringResponseTypeTests extends AbstractOidcTests {
        @Test
        public void verifyTokenGenerationWithClaimsForCodeResponseType() throws Exception {
            val request = new MockHttpServletRequest();
            val profile = new CommonProfile();
            profile.setClientName("OIDC");
            profile.setId("casuser");

            request.setAttribute(Pac4jConstants.USER_PROFILES,
                CollectionUtils.wrapLinkedHashMap(profile.getClientName(), profile));

            val phoneValues = List.of("123456789", "4805553241");
            val principal = RegisteredServiceTestUtils.getPrincipal("casuser", CollectionUtils.wrap(
                OIDC_CLAIM_EMAIL, List.of("casuser@example.org"),
                "color", List.of("yellow"),
                OIDC_CLAIM_PHONE_NUMBER, phoneValues,
                OIDC_CLAIM_NAME, List.of("casuser")));

            val authentication = CoreAuthenticationTestUtils.getAuthentication(principal,
                CollectionUtils.wrap(OAuth20Constants.STATE, List.of("some-state"),
                    OAuth20Constants.NONCE, List.of("some-nonce")));

            val tgt = new MockTicketGrantingTicket(authentication);
            val callback = casProperties.getServer().getPrefix()
                           + OAuth20Constants.BASE_OAUTH20_URL + '/'
                           + OAuth20Constants.CALLBACK_AUTHORIZE_URL_DEFINITION;

            val service = new WebApplicationServiceFactory().createService(callback);
            tgt.getServices().putAll(CollectionUtils.wrap("service", service));

            val accessToken = mock(OAuth20AccessToken.class);
            when(accessToken.getAuthentication()).thenReturn(authentication);
            when(accessToken.getTicketGrantingTicket()).thenReturn(tgt);
            when(accessToken.getId()).thenReturn(getClass().getSimpleName());
            when(accessToken.getScopes()).thenReturn(Set.of(OPENID.getScope(), PROFILE.getScope(), EMAIL.getScope(), PHONE.getScope()));

            val registeredService = (OidcRegisteredService) OAuth20Utils.getRegisteredOAuthServiceByClientId(servicesManager, "clientid");
            registeredService.setScopes(CollectionUtils.wrapSet(EMAIL.getScope(), PROFILE.getScope(), PHONE.getScope()));
            val idToken = oidcIdTokenGenerator.generate(accessToken, profile,
                OAuth20ResponseTypes.CODE, OAuth20GrantTypes.NONE, registeredService);
            assertNotNull(idToken);

            val claims = oidcTokenSigningAndEncryptionService.decode(idToken, Optional.ofNullable(registeredService));
            assertNotNull(claims);
            assertTrue(claims.hasClaim(OIDC_CLAIM_EMAIL));
            assertTrue(claims.hasClaim(OIDC_CLAIM_NAME));
            assertTrue(claims.hasClaim(OIDC_CLAIM_PHONE_NUMBER));
        }
    }

    @Nested
    @SuppressWarnings("ClassCanBeStatic")
    @TestPropertySource(properties = {
        "cas.authn.oidc.id-token.include-id-token-claims=false",
        "cas.authn.oauth.access-token.crypto.encryption-enabled=false",
        "cas.authn.oidc.core.claims-map.preferred_username=custom-attribute"
    })
    public class DefaultTests extends AbstractOidcTests {
        @Test
        public void verifyTokenGeneration() throws Exception {
            val request = new MockHttpServletRequest();
            val profile = new CommonProfile();
            profile.setClientName("OIDC");
            profile.setId("casuser");

            request.setAttribute(Pac4jConstants.USER_PROFILES,
                CollectionUtils.wrapLinkedHashMap(profile.getClientName(), profile));

            val phoneValues = List.of("\\\\123456789", "4805553241");
            val principal = RegisteredServiceTestUtils.getPrincipal("casuser", CollectionUtils.wrap(
                OIDC_CLAIM_EMAIL, List.of("casuser@example.org"),
                OIDC_CLAIM_PHONE_NUMBER, phoneValues,
                "color", List.of("yellow"),
                "custom-attribute", "test",
                OIDC_CLAIM_NAME, List.of("casuser")));

            val authentication = CoreAuthenticationTestUtils.getAuthentication(principal,
                CollectionUtils.wrap(OAuth20Constants.STATE, List.of("some-state"),
                    OAuth20Constants.NONCE, List.of("some-nonce")));
            val tgt = new MockTicketGrantingTicket(authentication);
            val callback = casProperties.getServer().getPrefix()
                           + OAuth20Constants.BASE_OAUTH20_URL + '/'
                           + OAuth20Constants.CALLBACK_AUTHORIZE_URL_DEFINITION;
            val service = new WebApplicationServiceFactory().createService(callback);
            tgt.getServices().putAll(CollectionUtils.wrap("service", service));

            val accessToken = mock(OAuth20AccessToken.class);
            when(accessToken.getAuthentication()).thenReturn(authentication);
            when(accessToken.getTicketGrantingTicket()).thenReturn(tgt);
            when(accessToken.getId()).thenReturn(getClass().getSimpleName());
            when(accessToken.getScopes()).thenReturn(Set.of(OPENID.getScope(), PROFILE.getScope(), EMAIL.getScope(), PHONE.getScope()));

            val registeredService = getOidcRegisteredService("clientid");
            registeredService.setIdTokenIssuer(UUID.randomUUID().toString());
            registeredService.setScopes(CollectionUtils.wrapSet(EMAIL.getScope(), PROFILE.getScope(), PHONE.getScope()));
            servicesManager.save(registeredService);

            val idToken = oidcIdTokenGenerator.generate(accessToken, profile,
                OAuth20ResponseTypes.ID_TOKEN, OAuth20GrantTypes.NONE, registeredService);
            assertNotNull(idToken);

            val claims = oidcTokenSigningAndEncryptionService.decode(idToken, Optional.ofNullable(registeredService));
            assertNotNull(claims);
            assertTrue(claims.hasClaim(OIDC_CLAIM_EMAIL));
            assertEquals(authentication.getAuthenticationDate().toEpochSecond(), (long) claims.getClaimValue(OidcConstants.CLAIM_AUTH_TIME));
            assertTrue(claims.hasClaim(OIDC_CLAIM_NAME));
            assertTrue(claims.hasClaim(OIDC_CLAIM_PHONE_NUMBER));
            assertTrue(claims.hasClaim(OIDC_CLAIM_PREFERRED_USERNAME));
            assertEquals("casuser@example.org", claims.getStringClaimValue(OIDC_CLAIM_EMAIL));
            assertEquals("casuser", claims.getStringClaimValue(OIDC_CLAIM_NAME));
            assertEquals(phoneValues, claims.getStringListClaimValue(OIDC_CLAIM_PHONE_NUMBER));
            assertEquals("test", claims.getStringClaimValue(OIDC_CLAIM_PREFERRED_USERNAME));
            assertEquals(registeredService.getIdTokenIssuer(), claims.getStringClaimValue(OidcConstants.ISS));
        }

        @Test
        public void verifyTokenGenerationWithoutClaimsForCodeResponseType() throws Exception {
            val request = new MockHttpServletRequest();
            val profile = new CommonProfile();
            profile.setClientName("OIDC");
            profile.setId("casuser");

            request.setAttribute(Pac4jConstants.USER_PROFILES,
                CollectionUtils.wrapLinkedHashMap(profile.getClientName(), profile));

            val phoneValues = List.of("123456789", "4805553241");
            val principal = RegisteredServiceTestUtils.getPrincipal("casuser", CollectionUtils.wrap(
                OIDC_CLAIM_EMAIL, List.of("casuser@example.org"),
                OIDC_CLAIM_PHONE_NUMBER, phoneValues,
                "color", List.of("yellow"),
                OIDC_CLAIM_NAME, List.of("casuser")));

            val authentication = CoreAuthenticationTestUtils.getAuthentication(principal,
                CollectionUtils.wrap(OAuth20Constants.STATE, List.of("some-state"),
                    OAuth20Constants.NONCE, List.of("some-nonce")));
            val tgt = new MockTicketGrantingTicket(authentication);
            val callback = casProperties.getServer().getPrefix()
                           + OAuth20Constants.BASE_OAUTH20_URL + '/'
                           + OAuth20Constants.CALLBACK_AUTHORIZE_URL_DEFINITION;
            val service = new WebApplicationServiceFactory().createService(callback);
            tgt.getServices().putAll(CollectionUtils.wrap("service", service));

            val accessToken = mock(OAuth20AccessToken.class);
            when(accessToken.getAuthentication()).thenReturn(authentication);
            when(accessToken.getTicketGrantingTicket()).thenReturn(tgt);
            when(accessToken.getId()).thenReturn(getClass().getSimpleName());
            when(accessToken.getScopes()).thenReturn(Set.of(OPENID.getScope(), PROFILE.getScope(), EMAIL.getScope(), PHONE.getScope()));

            val registeredService = (OidcRegisteredService) OAuth20Utils.getRegisteredOAuthServiceByClientId(servicesManager, "clientid");
            registeredService.setScopes(CollectionUtils.wrapSet(EMAIL.getScope(), PROFILE.getScope(), PHONE.getScope()));
            val idToken = oidcIdTokenGenerator.generate(accessToken, profile,
                OAuth20ResponseTypes.CODE, OAuth20GrantTypes.NONE, registeredService);
            assertNotNull(idToken);

            val claims = oidcTokenSigningAndEncryptionService.decode(idToken, Optional.ofNullable(registeredService));
            assertNotNull(claims);
            assertFalse(claims.hasClaim(OIDC_CLAIM_EMAIL));
            assertFalse(claims.hasClaim(OIDC_CLAIM_NAME));
            assertFalse(claims.hasClaim(OIDC_CLAIM_PHONE_NUMBER));
        }

        @Test
        public void verifyTokenGenerationWithOutClaimsForAuthzCodeGrantType() throws Exception {
            val request = new MockHttpServletRequest();
            val profile = new CommonProfile();
            profile.setClientName("OIDC");
            profile.setId("casuser");

            request.setAttribute(Pac4jConstants.USER_PROFILES,
                CollectionUtils.wrapLinkedHashMap(profile.getClientName(), profile));

            val phoneValues = List.of("123456789", "4805553241");
            val principal = RegisteredServiceTestUtils.getPrincipal("casuser", CollectionUtils.wrap(
                OIDC_CLAIM_EMAIL, List.of("casuser@example.org"),
                "color", List.of("yellow"),
                OIDC_CLAIM_PHONE_NUMBER, phoneValues,
                OIDC_CLAIM_NAME, List.of("casuser")));

            var authentication = CoreAuthenticationTestUtils.getAuthentication(principal,
                CollectionUtils.wrap(OAuth20Constants.STATE, List.of("some-state"),
                    OAuth20Constants.NONCE, List.of("some-nonce")));
            val tgt = new MockTicketGrantingTicket(authentication);

            val callback = casProperties.getServer().getPrefix()
                           + OAuth20Constants.BASE_OAUTH20_URL + '/'
                           + OAuth20Constants.CALLBACK_AUTHORIZE_URL_DEFINITION;

            val service = new WebApplicationServiceFactory().createService(callback);
            tgt.getServices().putAll(CollectionUtils.wrap("service", service));

            val accessToken = mock(OAuth20AccessToken.class);
            when(accessToken.getAuthentication()).thenReturn(authentication);
            when(accessToken.getTicketGrantingTicket()).thenReturn(tgt);
            when(accessToken.getId()).thenReturn(getClass().getSimpleName());
            when(accessToken.getScopes()).thenReturn(Set.of(OPENID.getScope(), PROFILE.getScope(), EMAIL.getScope(), PHONE.getScope()));

            val registeredService = (OidcRegisteredService) OAuth20Utils.getRegisteredOAuthServiceByClientId(servicesManager, "clientid");
            assertNotNull(registeredService);
            registeredService.setScopes(CollectionUtils.wrapSet(EMAIL.getScope(), PROFILE.getScope(), PHONE.getScope()));
            val idToken = oidcIdTokenGenerator.generate(accessToken, profile,
                OAuth20ResponseTypes.ID_TOKEN, OAuth20GrantTypes.AUTHORIZATION_CODE, registeredService);
            assertNotNull(idToken);
            val claims = oidcTokenSigningAndEncryptionService.decode(idToken, Optional.of(registeredService));
            assertNotNull(claims);
            assertFalse(claims.hasClaim(OIDC_CLAIM_EMAIL));
            assertFalse(claims.hasClaim(OIDC_CLAIM_NAME));
            assertFalse(claims.hasClaim(OIDC_CLAIM_PHONE_NUMBER));
        }

        @Test
        public void verifyTokenGenerationWithoutCallbackService() throws Exception {
            val request = new MockHttpServletRequest();
            val profile = new CommonProfile();
            profile.setClientName("OIDC");
            profile.setId("casuser");
            request.setAttribute(Pac4jConstants.USER_PROFILES,
                CollectionUtils.wrapLinkedHashMap(profile.getClientName(), profile));

            val mfa = casProperties.getAuthn().getMfa();
            val authentication = CoreAuthenticationTestUtils.getAuthentication("casuser",
                CollectionUtils.wrap(OAuth20Constants.STATE, List.of("some-state"),
                    OAuth20Constants.NONCE, List.of("some-nonce"),
                    mfa.getCore().getAuthenticationContextAttribute(), List.of("context-cass"),
                    AuthenticationHandler.SUCCESSFUL_AUTHENTICATION_HANDLERS, List.of("Handler1")));

            val tgt = new MockTicketGrantingTicket(authentication);
            val accessToken = mock(OAuth20AccessToken.class);
            when(accessToken.getAuthentication()).thenReturn(authentication);
            when(accessToken.getTicketGrantingTicket()).thenReturn(tgt);
            when(accessToken.getId()).thenReturn(getClass().getSimpleName());

            val idToken = oidcIdTokenGenerator.generate(accessToken, profile,
                OAuth20ResponseTypes.CODE, OAuth20GrantTypes.NONE,
                OAuth20Utils.getRegisteredOAuthServiceByClientId(servicesManager, "clientid"));
            assertNotNull(idToken);
        }

        @Test
        public void verifyUnknownServiceType() {
            assertThrows(IllegalArgumentException.class, () -> {
                val accessToken = mock(OAuth20AccessToken.class);
                oidcIdTokenGenerator.generate(accessToken, new CommonProfile(),
                    OAuth20ResponseTypes.CODE, OAuth20GrantTypes.NONE, new MockOAuthRegisteredService());
            });
        }

        @RepeatedTest(2)
        public void verifyAccessTokenAsJwt(final RepetitionInfo repetitionInfo) throws Exception {
            val request = new MockHttpServletRequest();
            val profile = new CommonProfile();
            profile.setClientName("OIDC");
            profile.setId("casuser");
            request.setAttribute(Pac4jConstants.USER_PROFILES,
                CollectionUtils.wrapLinkedHashMap(profile.getClientName(), profile));

            val accessToken = getAccessToken();
            val registeredService = getOidcRegisteredService(accessToken.getClientId());
            registeredService.setJwtAccessToken(true);
            registeredService.setIdTokenSigningAlg(AlgorithmIdentifiers.RSA_USING_SHA256);

            registeredService.setProperties(Map.of(
                RegisteredServiceProperty.RegisteredServiceProperties.ACCESS_TOKEN_AS_JWT_ENCRYPTION_ENABLED.getPropertyName(),
                new DefaultRegisteredServiceProperty("false"),
                RegisteredServiceProperty.RegisteredServiceProperties.ACCESS_TOKEN_AS_JWT_SIGNING_ENABLED.getPropertyName(),
                new DefaultRegisteredServiceProperty(repetitionInfo.getCurrentRepetition() % 2 == 0 ? "false" : "true")
            ));

            servicesManager.save(registeredService);
            val idToken = oidcIdTokenGenerator.generate(accessToken, profile, OAuth20ResponseTypes.CODE, OAuth20GrantTypes.NONE, registeredService);
            assertNotNull(idToken);
            val claims = oidcTokenSigningAndEncryptionService.decode(idToken, Optional.of(registeredService));
            assertNotNull(claims);
            assertTrue(claims.hasClaim(OidcConstants.CLAIM_AT_HASH));
            assertTrue(claims.hasClaim(OidcConstants.CLAIM_AUTH_TIME));
            val issuer = oidcIssuerService.determineIssuer(Optional.of(registeredService));
            assertEquals(issuer, claims.getIssuer());

            val hash = claims.getClaimValue(OidcConstants.CLAIM_AT_HASH, String.class);
            val encodedAccessToken = OAuth20JwtAccessTokenEncoder.builder()
                .accessToken(accessToken)
                .registeredService(registeredService)
                .service(accessToken.getService())
                .casProperties(casProperties)
                .accessTokenJwtBuilder(oidcAccessTokenJwtBuilder)
                .issuer(issuer)
                .build()
                .encode(accessToken.getId());
            val newHash = OAuth20AccessTokenAtHashGenerator.builder()
                .encodedAccessToken(encodedAccessToken)
                .registeredService(registeredService)
                .algorithm(registeredService.getIdTokenSigningAlg())
                .build()
                .generate();
            assertEquals(hash, newHash);
        }
    }
}
