package org.apereo.cas.oidc;

import org.apereo.cas.audit.AuditableExecution;
import org.apereo.cas.audit.spi.config.CasCoreAuditConfiguration;
import org.apereo.cas.authentication.MultifactorAuthenticationTrigger;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.authentication.principal.WebApplicationServiceFactory;
import org.apereo.cas.config.CasCoreAuthenticationConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationHandlersConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationMetadataConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationPolicyConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationPrincipalConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationServiceSelectionStrategyConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationSupportConfiguration;
import org.apereo.cas.config.CasCoreConfiguration;
import org.apereo.cas.config.CasCoreHttpConfiguration;
import org.apereo.cas.config.CasCoreMultifactorAuthenticationConfiguration;
import org.apereo.cas.config.CasCoreNotificationsConfiguration;
import org.apereo.cas.config.CasCoreServicesAuthenticationConfiguration;
import org.apereo.cas.config.CasCoreServicesConfiguration;
import org.apereo.cas.config.CasCoreTicketCatalogConfiguration;
import org.apereo.cas.config.CasCoreTicketIdGeneratorsConfiguration;
import org.apereo.cas.config.CasCoreTicketsConfiguration;
import org.apereo.cas.config.CasCoreTicketsSerializationConfiguration;
import org.apereo.cas.config.CasCoreUtilConfiguration;
import org.apereo.cas.config.CasCoreUtilSerializationConfiguration;
import org.apereo.cas.config.CasCoreWebConfiguration;
import org.apereo.cas.config.CasDefaultServiceTicketIdGeneratorsConfiguration;
import org.apereo.cas.config.CasOAuth20AuthenticationServiceSelectionStrategyConfiguration;
import org.apereo.cas.config.CasOAuth20Configuration;
import org.apereo.cas.config.CasOAuth20EndpointsConfiguration;
import org.apereo.cas.config.CasOAuth20ProtocolTicketCatalogConfiguration;
import org.apereo.cas.config.CasOAuth20ThrottleConfiguration;
import org.apereo.cas.config.CasPersonDirectoryTestConfiguration;
import org.apereo.cas.config.CasRegisteredServicesTestConfiguration;
import org.apereo.cas.config.CasThrottlingConfiguration;
import org.apereo.cas.config.CasThymeleafConfiguration;
import org.apereo.cas.config.support.CasWebApplicationServiceFactoryConfiguration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.logout.config.CasCoreLogoutConfiguration;
import org.apereo.cas.logout.slo.SingleLogoutServiceLogoutUrlBuilder;
import org.apereo.cas.mock.MockTicketGrantingTicket;
import org.apereo.cas.oidc.claims.OidcAttributeToScopeClaimMapper;
import org.apereo.cas.oidc.config.OidcComponentSerializationConfiguration;
import org.apereo.cas.oidc.config.OidcConfiguration;
import org.apereo.cas.oidc.config.OidcEndpointsConfiguration;
import org.apereo.cas.oidc.config.OidcJwksConfiguration;
import org.apereo.cas.oidc.config.OidcLogoutConfiguration;
import org.apereo.cas.oidc.config.OidcResponseConfiguration;
import org.apereo.cas.oidc.config.OidcThrottleConfiguration;
import org.apereo.cas.oidc.discovery.OidcServerDiscoverySettings;
import org.apereo.cas.oidc.discovery.webfinger.OidcWebFingerDiscoveryService;
import org.apereo.cas.oidc.issuer.OidcIssuerService;
import org.apereo.cas.oidc.jwks.OidcJsonWebKeyCacheKey;
import org.apereo.cas.oidc.jwks.generator.OidcJsonWebKeystoreGeneratorService;
import org.apereo.cas.oidc.jwks.rotation.OidcJsonWebKeystoreRotationService;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredServiceCipherExecutor;
import org.apereo.cas.services.RegisteredServiceLogoutType;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.services.ServiceRegistryListener;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.services.web.config.CasThemesConfiguration;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.OAuth20GrantTypes;
import org.apereo.cas.support.oauth.OAuth20ResponseTypes;
import org.apereo.cas.support.oauth.profile.OAuth20ProfileScopeToAttributesFilter;
import org.apereo.cas.support.oauth.profile.OAuth20UserProfileDataCreator;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.support.oauth.web.OAuth20RequestParameterResolver;
import org.apereo.cas.support.oauth.web.response.OAuth20CasClientRedirectActionBuilder;
import org.apereo.cas.support.oauth.web.response.accesstoken.response.OAuth20AccessTokenResponseGenerator;
import org.apereo.cas.support.oauth.web.response.callback.OAuth20AuthorizationResponseBuilder;
import org.apereo.cas.support.oauth.web.response.callback.OAuth20ResponseModeFactory;
import org.apereo.cas.support.oauth.web.views.ConsentApprovalViewResolver;
import org.apereo.cas.support.oauth.web.views.OAuth20CallbackAuthorizeViewResolver;
import org.apereo.cas.support.oauth.web.views.OAuth20UserProfileViewRenderer;
import org.apereo.cas.ticket.ExpirationPolicyBuilder;
import org.apereo.cas.ticket.IdTokenGeneratorService;
import org.apereo.cas.ticket.OAuth20TokenSigningAndEncryptionService;
import org.apereo.cas.ticket.TicketFactory;
import org.apereo.cas.ticket.accesstoken.OAuth20AccessToken;
import org.apereo.cas.ticket.code.OAuth20Code;
import org.apereo.cas.ticket.code.OAuth20CodeFactory;
import org.apereo.cas.ticket.device.OAuth20DeviceTokenFactory;
import org.apereo.cas.ticket.device.OAuth20DeviceUserCodeFactory;
import org.apereo.cas.ticket.expiration.NeverExpiresExpirationPolicy;
import org.apereo.cas.ticket.refreshtoken.OAuth20RefreshToken;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.token.JwtBuilder;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.RandomUtils;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.util.spring.ApplicationContextProvider;
import org.apereo.cas.util.spring.CasEventListener;
import org.apereo.cas.web.config.CasCookieConfiguration;
import org.apereo.cas.web.cookie.CasCookieBuilder;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.config.CasCoreWebflowConfiguration;
import org.apereo.cas.web.flow.config.CasMultifactorAuthenticationWebflowConfiguration;
import org.apereo.cas.web.flow.config.CasWebflowContextConfiguration;

import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.junit.jupiter.api.BeforeEach;
import org.pac4j.core.context.session.SessionStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.webflow.execution.Action;

import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * This is {@link AbstractOidcTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@SpringBootTest(classes = AbstractOidcTests.SharedTestConfiguration.class,
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.mvc.pathmatch.matching-strategy=ant-path-matcher",
        "cas.authn.oidc.core.issuer=https://sso.example.org/cas/oidc",
        "cas.authn.oidc.jwks.file-system.jwks-file=classpath:keystore.jwks"
    })
@EnableConfigurationProperties(CasConfigurationProperties.class)
public abstract class AbstractOidcTests {
    protected static final String TGT_ID = "TGT-0";

    @Autowired
    @Qualifier(OAuth20ResponseModeFactory.BEAN_NAME)
    protected OAuth20ResponseModeFactory oauthResponseModeFactory;
    @Autowired
    @Qualifier(OAuth20RequestParameterResolver.BEAN_NAME)
    protected OAuth20RequestParameterResolver oauthRequestParameterResolver;
    @Autowired
    @Qualifier(CasCookieBuilder.BEAN_NAME_TICKET_GRANTING_COOKIE_BUILDER)
    protected CasCookieBuilder ticketGrantingTicketCookieGenerator;

    @Autowired
    @Qualifier("oidcMultifactorAuthenticationTrigger")
    protected MultifactorAuthenticationTrigger oidcMultifactorAuthenticationTrigger;

    @Autowired
    @Qualifier(OidcIssuerService.BEAN_NAME)
    protected OidcIssuerService oidcIssuerService;

    @Autowired
    @Qualifier("oidcJsonWebKeystoreRotationService")
    protected OidcJsonWebKeystoreRotationService oidcJsonWebKeystoreRotationService;

    @Autowired
    @Qualifier("singleLogoutServiceLogoutUrlBuilder")
    protected SingleLogoutServiceLogoutUrlBuilder singleLogoutServiceLogoutUrlBuilder;

    protected ConfigurableApplicationContext applicationContext;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Autowired
    @Qualifier("oauthDistributedSessionStore")
    protected SessionStore oauthDistributedSessionStore;
    @Autowired
    @Qualifier("oauthInterceptor")
    protected HandlerInterceptor oauthInterceptor;

    @Autowired
    @Qualifier("oidcWebFingerDiscoveryService")
    protected OidcWebFingerDiscoveryService oidcWebFingerDiscoveryService;

    @Autowired
    @Qualifier("oidcImplicitIdTokenAndTokenCallbackUrlBuilder")
    protected OAuth20AuthorizationResponseBuilder oidcImplicitIdTokenAndTokenCallbackUrlBuilder;

    @Autowired
    @Qualifier("oidcImplicitIdTokenCallbackUrlBuilder")
    protected OAuth20AuthorizationResponseBuilder oidcImplicitIdTokenCallbackUrlBuilder;

    @Autowired
    @Qualifier("oidcRegisteredServiceJwtAccessTokenCipherExecutor")
    protected RegisteredServiceCipherExecutor oidcRegisteredServiceJwtAccessTokenCipherExecutor;

    @Autowired
    @Qualifier("oidcAccessTokenJwtCipherExecutor")
    protected CipherExecutor<Serializable, String> oidcAccessTokenJwtCipherExecutor;

    @Autowired
    @Qualifier("oidcResponseModeJwtCipherExecutor")
    protected CipherExecutor<Serializable, String> oidcResponseModeJwtCipherExecutor;

    @Autowired
    @Qualifier("oidcUserProfileViewRenderer")
    protected OAuth20UserProfileViewRenderer oidcUserProfileViewRenderer;

    @Autowired
    @Qualifier("defaultDeviceTokenFactory")
    protected OAuth20DeviceTokenFactory deviceTokenFactory;

    @Autowired
    @Qualifier("defaultDeviceUserCodeFactory")
    protected OAuth20DeviceUserCodeFactory deviceUserCodeFactory;

    @Autowired
    @Qualifier("oidcUserProfileDataCreator")
    protected OAuth20UserProfileDataCreator oidcUserProfileDataCreator;

    @Autowired
    @Qualifier("oauthCasClientRedirectActionBuilder")
    protected OAuth20CasClientRedirectActionBuilder oauthCasClientRedirectActionBuilder;

    @Autowired
    @Qualifier("profileScopeToAttributesFilter")
    protected OAuth20ProfileScopeToAttributesFilter profileScopeToAttributesFilter;

    @Autowired
    @Qualifier("oidcUserProfileSigningAndEncryptionService")
    protected OAuth20TokenSigningAndEncryptionService oidcUserProfileSigningAndEncryptionService;

    @Autowired
    @Qualifier("oidcServiceRegistryListener")
    protected ServiceRegistryListener oidcServiceRegistryListener;

    @Autowired
    @Qualifier("oidcJsonWebKeyStoreListener")
    protected CasEventListener oidcJsonWebKeyStoreListener;

    @Autowired
    @Qualifier("defaultOAuthCodeFactory")
    protected OAuth20CodeFactory defaultOAuthCodeFactory;

    @Autowired
    @Qualifier("webApplicationServiceFactory")
    protected ServiceFactory<WebApplicationService> webApplicationServiceFactory;

    @Autowired
    @Qualifier("callbackAuthorizeViewResolver")
    protected OAuth20CallbackAuthorizeViewResolver callbackAuthorizeViewResolver;

    @Autowired
    protected CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier(OidcConfigurationContext.BEAN_NAME)
    protected OidcConfigurationContext oidcConfigurationContext;

    @Autowired
    @Qualifier("oidcDefaultJsonWebKeystoreCache")
    protected LoadingCache<OidcJsonWebKeyCacheKey, Optional<JsonWebKeySet>> oidcDefaultJsonWebKeystoreCache;

    @Autowired
    @Qualifier("oidcTokenSigningAndEncryptionService")
    protected OAuth20TokenSigningAndEncryptionService oidcTokenSigningAndEncryptionService;

    @Autowired
    @Qualifier("oidcServiceJsonWebKeystoreCache")
    protected LoadingCache<OidcJsonWebKeyCacheKey, Optional<JsonWebKeySet>> oidcServiceJsonWebKeystoreCache;

    @Autowired
    @Qualifier("oidcJsonWebKeystoreGeneratorService")
    protected OidcJsonWebKeystoreGeneratorService oidcJsonWebKeystoreGeneratorService;

    @Autowired
    @Qualifier(AuditableExecution.AUDITABLE_EXECUTION_REGISTERED_SERVICE_ACCESS)
    protected AuditableExecution registeredServiceAccessStrategyEnforcer;

    @Autowired
    @Qualifier(CasWebflowConstants.ACTION_ID_OIDC_REGSTERED_SERVICE_UI)
    protected Action oidcRegisteredServiceUIAction;

    @Autowired
    @Qualifier(OidcServerDiscoverySettings.BEAN_NAME_FACTORY)
    protected OidcServerDiscoverySettings oidcServerDiscoverySettings;

    @Autowired
    @Qualifier("oidcAccessTokenResponseGenerator")
    protected OAuth20AccessTokenResponseGenerator oidcAccessTokenResponseGenerator;

    @Autowired
    @Qualifier(OidcAttributeToScopeClaimMapper.DEFAULT_BEAN_NAME)
    protected OidcAttributeToScopeClaimMapper oidcAttributeToScopeClaimMapper;

    @Autowired
    @Qualifier(TicketFactory.BEAN_NAME)
    protected TicketFactory defaultTicketFactory;

    @Autowired
    @Qualifier(TicketRegistry.BEAN_NAME)
    protected TicketRegistry ticketRegistry;

    @Autowired
    @Qualifier(ServicesManager.BEAN_NAME)
    protected ServicesManager servicesManager;

    @Autowired
    @Qualifier("oidcIdTokenGenerator")
    protected IdTokenGeneratorService oidcIdTokenGenerator;

    @Autowired
    @Qualifier("consentApprovalViewResolver")
    protected ConsentApprovalViewResolver consentApprovalViewResolver;

    @Autowired
    @Qualifier("accessTokenJwtBuilder")
    protected JwtBuilder oidcAccessTokenJwtBuilder;

    @Autowired
    @Qualifier("accessTokenExpirationPolicy")
    protected ExpirationPolicyBuilder accessTokenExpirationPolicy;

    protected static OidcRegisteredService getOidcRegisteredService() {
        return getOidcRegisteredService(true, true);
    }

    protected static OidcRegisteredService getOidcRegisteredService(final boolean sign,
                                                                    final boolean encrypt) {
        return getOidcRegisteredService("clientid", "https://oauth\\.example\\.org.*", sign, encrypt);
    }

    protected static OidcRegisteredService getOidcRegisteredService(final String clientid, final String redirectUri) {
        return getOidcRegisteredService(clientid, redirectUri, true, true);
    }

    protected static OidcRegisteredService getOidcRegisteredService(final String clientid) {
        return getOidcRegisteredService(clientid, "https://oauth\\.example\\.org.*", true, true);
    }

    protected static OidcRegisteredService getOidcRegisteredService(final String clientId,
                                                                    final String redirectUri,
                                                                    final boolean sign,
                                                                    final boolean encrypt) {
        val svc = new OidcRegisteredService();
        svc.setClientId(clientId);
        svc.setName("oauth");
        svc.setDescription("description");
        svc.setClientSecret("secret");
        svc.setServiceId(redirectUri);
        svc.setSignIdToken(sign);
        svc.setEncryptIdToken(encrypt);
        svc.setIdTokenEncryptionAlg(KeyManagementAlgorithmIdentifiers.RSA_OAEP_256);
        svc.setIdTokenEncryptionEncoding(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
        svc.setInformationUrl("info");
        svc.setPrivacyUrl("privacy");
        svc.setJwks("classpath:keystore.jwks");
        svc.setLogoutUrl("https://oauth.example.org/logout,https://logout,https://www.acme.com/.*");
        svc.setLogoutType(RegisteredServiceLogoutType.BACK_CHANNEL);
        svc.setScopes(CollectionUtils.wrapSet(OidcConstants.StandardScopes.EMAIL.getScope(),
            OidcConstants.StandardScopes.PROFILE.getScope()));
        return svc;
    }

    protected static OAuthRegisteredService getOAuthRegisteredService(final String clientId,
                                                                      final String redirectUri) {
        val svc = new OAuthRegisteredService();
        svc.setClientId(clientId);
        svc.setName("oauth");
        svc.setDescription("description");
        svc.setClientSecret("secret");
        svc.setServiceId(redirectUri);
        svc.setInformationUrl("info");
        svc.setPrivacyUrl("privacy");
        return svc;
    }

    protected static OAuth20RefreshToken getRefreshToken() {
        val principal = RegisteredServiceTestUtils.getPrincipal("casuser", CollectionUtils.wrap("email", List.of("casuser@example.org")));
        val token = mock(OAuth20RefreshToken.class);
        when(token.getAuthentication()).thenReturn(RegisteredServiceTestUtils.getAuthentication(principal));
        when(token.getService()).thenReturn(RegisteredServiceTestUtils.getService("https://oauth.example.org"));
        when(token.getId()).thenReturn("RT-123456");
        when(token.getTicketGrantingTicket()).thenReturn(new MockTicketGrantingTicket("casuser"));
        when(token.getScopes()).thenReturn(Set.of(OidcConstants.StandardScopes.EMAIL.getScope(),
            OidcConstants.StandardScopes.PROFILE.getScope(),
            OidcConstants.StandardScopes.OPENID.getScope()));
        when(token.getExpirationPolicy()).thenReturn(NeverExpiresExpirationPolicy.INSTANCE);
        return token;
    }

    protected static MockHttpServletRequest getHttpRequestForEndpoint(final String endpoint) {
        val request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("sso.example.org");
        request.setServerPort(443);
        request.setRequestURI("/cas/oidc/" + endpoint);
        return request;
    }

    @BeforeEach
    public void initialize() throws Exception {
        this.applicationContext = new StaticApplicationContext();
        applicationContext.refresh();
        ApplicationContextProvider.registerBeanIntoApplicationContext(applicationContext, casProperties,
            CasConfigurationProperties.class.getSimpleName());
        ApplicationContextProvider.registerBeanIntoApplicationContext(applicationContext, oidcAttributeToScopeClaimMapper,
            OidcAttributeToScopeClaimMapper.DEFAULT_BEAN_NAME);
        ApplicationContextProvider.holdApplicationContext(applicationContext);

        servicesManager.save(getOidcRegisteredService());
        ticketRegistry.deleteAll();
    }

    protected JwtClaims getClaims() {
        return getClaims(getOidcRegisteredService().getClientId());
    }

    protected JwtClaims getClaims(final String clientId) {
        return getClaims("casuser", casProperties.getAuthn().getOidc().getCore().getIssuer(), clientId, clientId);
    }

    protected JwtClaims getClaims(final String subject, final String issuer,
                                  final String clientId, final String audience) {
        val claims = new JwtClaims();
        claims.setJwtId(RandomUtils.randomAlphanumeric(16));
        claims.setIssuer(issuer);
        claims.setAudience(audience);

        val expirationDate = NumericDate.now();
        expirationDate.addSeconds(120);
        claims.setExpirationTime(expirationDate);
        claims.setIssuedAtToNow();
        claims.setNotBeforeMinutesInThePast(1);
        claims.setSubject(subject);
        claims.setStringClaim(OAuth20Constants.CLIENT_ID, clientId);
        return claims;
    }

    protected OAuth20AccessToken getAccessToken(final Principal principal) throws Exception {
        return getAccessToken(principal, StringUtils.EMPTY, "clientId");
    }

    protected OAuth20AccessToken getAccessToken() throws Exception {
        return getAccessToken(StringUtils.EMPTY, "clientId");
    }

    protected OAuth20AccessToken getAccessToken(final String clientId) throws Exception {
        return getAccessToken(StringUtils.EMPTY, clientId);
    }

    protected OAuth20AccessToken getAccessToken(final String idToken, final String clientId) throws Exception {
        val principal = RegisteredServiceTestUtils.getPrincipal("casuser", CollectionUtils.wrap("email", List.of("casuser@example.org")));
        return getAccessToken(principal, idToken, clientId);
    }

    protected OAuth20AccessToken getAccessToken(
        final Principal principal,
        final String idToken,
        final String clientId) throws Exception {
        val code = addCode(principal, getOidcRegisteredService());

        val accessToken = mock(OAuth20AccessToken.class);
        when(accessToken.getAuthentication()).thenReturn(RegisteredServiceTestUtils.getAuthentication(principal));
        when(accessToken.getService()).thenReturn(RegisteredServiceTestUtils.getService("https://oauth.example.org"));
        when(accessToken.getId()).thenReturn("AT-" + UUID.randomUUID());
        when(accessToken.getExpirationPolicy()).thenReturn(NeverExpiresExpirationPolicy.INSTANCE);
        when(accessToken.getTicketGrantingTicket()).thenReturn(new MockTicketGrantingTicket("casuser"));
        when(accessToken.getClientId()).thenReturn(clientId);
        when(accessToken.getCreationTime()).thenReturn(ZonedDateTime.now(ZoneOffset.UTC));
        when(accessToken.getScopes()).thenReturn(Set.of(OidcConstants.StandardScopes.EMAIL.getScope(),
            OidcConstants.StandardScopes.PROFILE.getScope(),
            OidcConstants.StandardScopes.OPENID.getScope()));
        when(accessToken.getToken()).thenReturn(code.getId());
        when(accessToken.getIdToken()).thenReturn(idToken);
        return accessToken;
    }


    protected OAuth20Code addCode(final Principal principal,
                                  final OAuthRegisteredService registeredService) throws Exception {
        val tgt = new MockTicketGrantingTicket("casuser");
        val authentication = RegisteredServiceTestUtils.getAuthentication(principal);
        val factory = new WebApplicationServiceFactory();
        val service = factory.createService(registeredService.getClientId());
        val code = defaultOAuthCodeFactory.create(service, authentication,
            tgt, new ArrayList<>(),
            null, null, "clientid", new HashMap<>(),
            OAuth20ResponseTypes.CODE, OAuth20GrantTypes.AUTHORIZATION_CODE);
        this.ticketRegistry.addTicket(code);
        return code;
    }

    @ImportAutoConfiguration({
        RefreshAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        WebMvcAutoConfiguration.class
    })
    @SpringBootConfiguration
    @Import({
        CasCoreNotificationsConfiguration.class,
        CasCoreServicesConfiguration.class,
        CasCoreUtilConfiguration.class,
        CasCoreUtilSerializationConfiguration.class,
        CasCoreWebflowConfiguration.class,
        CasCoreWebConfiguration.class,
        CasCoreConfiguration.class,
        CasCoreTicketsConfiguration.class,
        CasCoreTicketCatalogConfiguration.class,
        CasCoreTicketIdGeneratorsConfiguration.class,
        CasDefaultServiceTicketIdGeneratorsConfiguration.class,
        CasCoreTicketsSerializationConfiguration.class,
        CasCoreHttpConfiguration.class,
        CasCoreAuditConfiguration.class,
        CasCoreLogoutConfiguration.class,
        CasWebflowContextConfiguration.class,
        CasCoreAuthenticationPrincipalConfiguration.class,
        CasPersonDirectoryTestConfiguration.class,
        CasRegisteredServicesTestConfiguration.class,
        CasCoreAuthenticationConfiguration.class,
        CasCookieConfiguration.class,
        CasThemesConfiguration.class,
        CasThymeleafConfiguration.class,
        CasWebApplicationServiceFactoryConfiguration.class,
        CasCoreAuthenticationHandlersConfiguration.class,
        CasCoreAuthenticationMetadataConfiguration.class,
        CasCoreAuthenticationPolicyConfiguration.class,
        CasCoreAuthenticationSupportConfiguration.class,
        CasCoreServicesAuthenticationConfiguration.class,
        CasThrottlingConfiguration.class,
        CasMultifactorAuthenticationWebflowConfiguration.class,
        CasCoreMultifactorAuthenticationConfiguration.class,
        CasCoreAuthenticationServiceSelectionStrategyConfiguration.class,
        OidcConfiguration.class,
        OidcJwksConfiguration.class,
        OidcEndpointsConfiguration.class,
        OidcResponseConfiguration.class,
        OidcLogoutConfiguration.class,
        OidcThrottleConfiguration.class,
        OidcComponentSerializationConfiguration.class,
        CasOAuth20Configuration.class,
        CasOAuth20ProtocolTicketCatalogConfiguration.class,
        CasOAuth20EndpointsConfiguration.class,
        CasOAuth20ThrottleConfiguration.class,
        CasOAuth20AuthenticationServiceSelectionStrategyConfiguration.class
    })
    public static class SharedTestConfiguration {
    }
}
