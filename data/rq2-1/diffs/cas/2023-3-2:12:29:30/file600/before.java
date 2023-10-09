package org.apereo.cas.support.oauth.authenticator;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.mock.MockTicketGrantingTicket;
import org.apereo.cas.services.RegisteredServiceAccessStrategyAuditableEnforcer;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.OAuth20GrantTypes;
import org.apereo.cas.support.oauth.OAuth20ResponseTypes;
import org.apereo.cas.ticket.InvalidTicketException;
import org.apereo.cas.ticket.code.OAuth20DefaultCode;
import org.apereo.cas.ticket.expiration.HardTimeoutExpirationPolicy;
import org.apereo.cas.util.DigestUtils;
import org.apereo.cas.util.EncodingUtils;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.jee.context.session.JEESessionStore;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link OAuth20ProofKeyCodeExchangeAuthenticatorTests}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0
 */
@Tag("OAuth")
public class OAuth20ProofKeyCodeExchangeAuthenticatorTests extends BaseOAuth20AuthenticatorTests {
    protected OAuth20ProofKeyCodeExchangeAuthenticator authenticator;

    @BeforeEach
    public void init() {
        authenticator = new OAuth20ProofKeyCodeExchangeAuthenticator(servicesManager, serviceFactory,
            new RegisteredServiceAccessStrategyAuditableEnforcer(new CasConfigurationProperties()), ticketRegistry,
            defaultPrincipalResolver, oauthRequestParameterResolver, oauth20ClientSecretValidator);
    }

    @Test
    public void verifyNoToken() {
        val credentials = new UsernamePasswordCredentials("clientWithoutSecret", "ABCD123");
        val request = new MockHttpServletRequest();
        request.addParameter(OAuth20Constants.CLIENT_ID, "clientWithoutSecret");
        request.addParameter(OAuth20Constants.CODE_VERIFIER, "ABCD123");
        request.addParameter(OAuth20Constants.CODE, "CODE-1234567890");
        val ctx = new JEEContext(request, new MockHttpServletResponse());
        assertThrows(InvalidTicketException.class,
            () -> authenticator.validate(credentials, ctx, JEESessionStore.INSTANCE));
    }


    @Test
    public void verifyAuthenticationPlainWithoutSecret() throws Exception {
        val credentials = new UsernamePasswordCredentials("clientWithoutSecret", "ABCD123");
        val request = new MockHttpServletRequest();
        ticketRegistry.addTicket(new OAuth20DefaultCode("CODE-1234567890",
            RegisteredServiceTestUtils.getService(), RegisteredServiceTestUtils.getAuthentication(),
            new HardTimeoutExpirationPolicy(10),
            new MockTicketGrantingTicket("casuser"),
            new ArrayList<>(), "ABCD123",
            "plain", "clientid12345", new HashMap<>(),
            OAuth20ResponseTypes.CODE, OAuth20GrantTypes.AUTHORIZATION_CODE));
        request.addParameter(OAuth20Constants.CLIENT_ID, "clientWithoutSecret");
        request.addParameter(OAuth20Constants.CODE_VERIFIER, "ABCD123");
        request.addParameter(OAuth20Constants.CODE, "CODE-1234567890");
        val ctx = new JEEContext(request, new MockHttpServletResponse());
        authenticator.validate(credentials, ctx, JEESessionStore.INSTANCE);
        assertNotNull(credentials.getUserProfile());
        assertEquals("clientWithoutSecret", credentials.getUserProfile().getId());
    }

    @Test
    public void verifyAuthenticationPlainWithSecretTransmittedByFormAuthn() throws Exception {
        val credentials = new UsernamePasswordCredentials("client", "ABCD123");
        val request = new MockHttpServletRequest();
        ticketRegistry.addTicket(
            new OAuth20DefaultCode("CODE-1234567890", RegisteredServiceTestUtils.getService(), RegisteredServiceTestUtils.getAuthentication(),
                new HardTimeoutExpirationPolicy(10),
                new MockTicketGrantingTicket("casuser"),
                new ArrayList<>(), "ABCD123",
                "plain", "clientid12345", new HashMap<>(),
                OAuth20ResponseTypes.CODE, OAuth20GrantTypes.AUTHORIZATION_CODE));
        request.addParameter(OAuth20Constants.CLIENT_ID, "client");
        request.addParameter(OAuth20Constants.CODE_VERIFIER, "ABCD123");
        request.addParameter(OAuth20Constants.CLIENT_SECRET, "secret");
        request.addParameter(OAuth20Constants.CODE, "CODE-1234567890");
        val ctx = new JEEContext(request, new MockHttpServletResponse());
        authenticator.validate(credentials, ctx, JEESessionStore.INSTANCE);
        assertNotNull(credentials.getUserProfile());
        assertEquals("client", credentials.getUserProfile().getId());
    }

    @Test
    public void verifyAuthenticationPlainWithSecretTransmittedByBasicAuthn() throws Exception {
        val credentials = new UsernamePasswordCredentials("client", "secret");
        val request = new MockHttpServletRequest();
        ticketRegistry.addTicket(
            new OAuth20DefaultCode("CODE-1234567890", RegisteredServiceTestUtils.getService(), RegisteredServiceTestUtils.getAuthentication(),
                new HardTimeoutExpirationPolicy(10),
                new MockTicketGrantingTicket("casuser"),
                new ArrayList<>(), "ABCD123",
                "plain", "clientid12345", new HashMap<>(),
                OAuth20ResponseTypes.CODE, OAuth20GrantTypes.AUTHORIZATION_CODE));
        request.addHeader("Authorization", "Basic " + EncodingUtils.encodeBase64("client:secret"));
        request.addParameter(OAuth20Constants.CODE_VERIFIER, "ABCD123");
        request.addParameter(OAuth20Constants.CODE, "CODE-1234567890");
        val ctx = new JEEContext(request, new MockHttpServletResponse());
        authenticator.validate(credentials, ctx, JEESessionStore.INSTANCE);
        assertNotNull(credentials.getUserProfile());
        assertEquals("client", credentials.getUserProfile().getId());
    }

    @Test
    public void verifyAuthenticationHashedWithoutSecret() throws Exception {
        val hash = EncodingUtils.encodeUrlSafeBase64(DigestUtils.rawDigestSha256("ABCD123"));
        val credentials = new UsernamePasswordCredentials("clientWithoutSecret", "ABCD123");
        val request = new MockHttpServletRequest();
        val ticket = new OAuth20DefaultCode("CODE-1234567890",
            RegisteredServiceTestUtils.getService(), RegisteredServiceTestUtils.getAuthentication(),
            new HardTimeoutExpirationPolicy(10),
            new MockTicketGrantingTicket("casuser"),
            new ArrayList<>(), hash, "s256", "clientid12345", new HashMap<>(),
            OAuth20ResponseTypes.CODE, OAuth20GrantTypes.AUTHORIZATION_CODE);
        ticketRegistry.addTicket(ticket);
        request.addParameter(OAuth20Constants.CLIENT_ID, "clientWithoutSecret");
        request.addParameter(OAuth20Constants.CODE_VERIFIER, "ABCD123");
        request.addParameter(OAuth20Constants.CODE, ticket.getId());
        val ctx = new JEEContext(request, new MockHttpServletResponse());
        authenticator.validate(credentials, ctx, JEESessionStore.INSTANCE);
        assertNotNull(credentials.getUserProfile());
        assertEquals("clientWithoutSecret", credentials.getUserProfile().getId());
    }

    @Test
    public void verifyUnknownDigest() throws Exception {
        val hash = EncodingUtils.encodeUrlSafeBase64(DigestUtils.rawDigestSha256("ABCD123"));
        val credentials = new UsernamePasswordCredentials("clientWithoutSecret", "ABCD123");
        val request = new MockHttpServletRequest();
        val ticket = new OAuth20DefaultCode("CODE-1234567890",
            RegisteredServiceTestUtils.getService(), RegisteredServiceTestUtils.getAuthentication(),
            new HardTimeoutExpirationPolicy(10),
            new MockTicketGrantingTicket("casuser"),
            new ArrayList<>(), hash, "unknown", "clientid12345", new HashMap<>(),
            OAuth20ResponseTypes.CODE, OAuth20GrantTypes.AUTHORIZATION_CODE);
        ticketRegistry.addTicket(ticket);
        request.addParameter(OAuth20Constants.CLIENT_ID, "clientWithoutSecret");
        request.addParameter(OAuth20Constants.CODE_VERIFIER, "ABCD123");
        request.addParameter(OAuth20Constants.CODE, ticket.getId());
        val ctx = new JEEContext(request, new MockHttpServletResponse());
        assertThrows(CredentialsException.class,
            () -> authenticator.validate(credentials, ctx, JEESessionStore.INSTANCE));
    }

    @Test
    public void verifyAuthenticationHashedWithSecretTransmittedByFormAuthn() throws Exception {
        val hash = EncodingUtils.encodeUrlSafeBase64(DigestUtils.rawDigestSha256("ABCD123"));
        val credentials = new UsernamePasswordCredentials("client", "ABCD123");
        val request = new MockHttpServletRequest();
        val ticket = new OAuth20DefaultCode("CODE-1234567890",
            RegisteredServiceTestUtils.getService(), RegisteredServiceTestUtils.getAuthentication(),
            new HardTimeoutExpirationPolicy(10),
            new MockTicketGrantingTicket("casuser"),
            new ArrayList<>(), hash, "s256", "clientid12345", new HashMap<>(),
            OAuth20ResponseTypes.CODE, OAuth20GrantTypes.AUTHORIZATION_CODE);
        ticketRegistry.addTicket(ticket);
        request.addParameter(OAuth20Constants.CLIENT_ID, "client");
        request.addParameter(OAuth20Constants.CODE_VERIFIER, "ABCD123");
        request.addParameter(OAuth20Constants.CLIENT_SECRET, "secret");
        request.addParameter(OAuth20Constants.CODE, ticket.getId());
        val ctx = new JEEContext(request, new MockHttpServletResponse());
        authenticator.validate(credentials, ctx, JEESessionStore.INSTANCE);
        assertNotNull(credentials.getUserProfile());
        assertEquals("client", credentials.getUserProfile().getId());
    }

    @Test
    public void verifyAuthenticationHashedWithSecretTransmittedByBasicFormAuthn() throws Exception {
        val hash = EncodingUtils.encodeUrlSafeBase64(DigestUtils.rawDigestSha256("ABCD123"));
        val credentials = new UsernamePasswordCredentials("client", "ABCD123");
        val request = new MockHttpServletRequest();
        val ticket = new OAuth20DefaultCode("CODE-1234567890",
            RegisteredServiceTestUtils.getService(), RegisteredServiceTestUtils.getAuthentication(),
            new HardTimeoutExpirationPolicy(10),
            new MockTicketGrantingTicket("casuser"),
            new ArrayList<>(), hash, "s256", "clientid12345", new HashMap<>(),
            OAuth20ResponseTypes.CODE, OAuth20GrantTypes.AUTHORIZATION_CODE);
        ticketRegistry.addTicket(ticket);
        request.addHeader("Authorization", "Basic " + EncodingUtils.encodeBase64("client:secret"));
        request.addHeader("Authorization", "Basic " + EncodingUtils.encodeBase64("client:secret"));
        request.addParameter(OAuth20Constants.CODE_VERIFIER, "ABCD123");
        request.addParameter(OAuth20Constants.CODE, ticket.getId());
        val ctx = new JEEContext(request, new MockHttpServletResponse());
        authenticator.validate(credentials, ctx, JEESessionStore.INSTANCE);
        assertNotNull(credentials.getUserProfile());
        assertEquals("client", credentials.getUserProfile().getId());
    }

    @Test
    public void verifyAuthenticationNotHashedCorrectly() throws Exception {
        val credentials = new UsernamePasswordCredentials("client", "ABCD123");
        val request = new MockHttpServletRequest();
        val ticket = new OAuth20DefaultCode("CODE-1234567890",
            RegisteredServiceTestUtils.getService(), RegisteredServiceTestUtils.getAuthentication(),
            new HardTimeoutExpirationPolicy(10),
            new MockTicketGrantingTicket("casuser"),
            new ArrayList<>(),
            "something-else", "s256", "clientid12345", new HashMap<>(),
            OAuth20ResponseTypes.CODE, OAuth20GrantTypes.AUTHORIZATION_CODE);
        ticketRegistry.addTicket(ticket);
        request.addParameter(OAuth20Constants.CLIENT_ID, "client");
        request.addParameter(OAuth20Constants.CODE_VERIFIER, "ABCD123");
        request.addParameter(OAuth20Constants.CLIENT_SECRET, "secret");
        request.addParameter(OAuth20Constants.CODE, ticket.getId());
        val ctx = new JEEContext(request, new MockHttpServletResponse());
        assertThrows(CredentialsException.class, () -> authenticator.validate(credentials, ctx, JEESessionStore.INSTANCE));
    }
}
