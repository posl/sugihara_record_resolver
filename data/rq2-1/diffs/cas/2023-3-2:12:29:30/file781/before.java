package org.apereo.cas.web.report;

import org.apereo.cas.CentralAuthenticationService;
import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.logout.slo.SingleLogoutRequestExecutor;
import org.apereo.cas.mock.MockTicketGrantingTicket;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.spring.DirectObjectProvider;

import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link SingleSignOnSessionsEndpointTests}.
 *
 * @author Misagh Moayyed
 * @since 6.2.0
 */
@TestPropertySource(properties = "management.endpoint.ssoSessions.enabled=true")
@Tag("ActuatorEndpoint")
public class SingleSignOnSessionsEndpointTests extends AbstractCasEndpointTests {
    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier("singleSignOnSessionsEndpoint")
    private SingleSignOnSessionsEndpoint singleSignOnSessionsEndpoint;

    @Autowired
    @Qualifier(SingleLogoutRequestExecutor.BEAN_NAME)
    private SingleLogoutRequestExecutor defaultSingleLogoutRequestExecutor;

    @Autowired
    @Qualifier(CentralAuthenticationService.BEAN_NAME)
    private CentralAuthenticationService centralAuthenticationService;

    @Autowired
    @Qualifier(TicketRegistry.BEAN_NAME)
    private TicketRegistry ticketRegistry;

    @BeforeEach
    public void setup() {
        val result = CoreAuthenticationTestUtils.getAuthenticationResult();
        val tgt = centralAuthenticationService.createTicketGrantingTicket(result);
        val st = centralAuthenticationService.grantServiceTicket(tgt.getId(), CoreAuthenticationTestUtils.getWebApplicationService(), result);
        assertNotNull(st);
    }

    @Test
    public void verifyDelete() {
        var results = singleSignOnSessionsEndpoint.destroySsoSessions(StringUtils.EMPTY, StringUtils.EMPTY,
            0, 1_000,
            new MockHttpServletRequest(), new MockHttpServletResponse());
        assertEquals(HttpStatus.BAD_REQUEST.value(), results.get("status"));
        results = singleSignOnSessionsEndpoint.destroySsoSessions(null, CoreAuthenticationTestUtils.CONST_USERNAME,
            0, 1_000,
            new MockHttpServletRequest(), new MockHttpServletResponse());
        assertFalse(results.isEmpty());

        results = singleSignOnSessionsEndpoint.destroySsoSession("unknown-ticket",
            new MockHttpServletRequest(), new MockHttpServletResponse());
        assertTrue(results.containsKey("status"));
        assertTrue(results.containsKey("ticketGrantingTicket"));

        val authResult = CoreAuthenticationTestUtils.getAuthenticationResult();
        val tgt = centralAuthenticationService.createTicketGrantingTicket(authResult);
        assertNotNull(tgt);
        results = singleSignOnSessionsEndpoint.destroySsoSessions(
            SingleSignOnSessionsEndpoint.SsoSessionReportOptions.ALL.getType(), null,
            0, 1_000,
            new MockHttpServletRequest(), new MockHttpServletResponse());
        assertFalse(results.isEmpty());
        assertNotNull(singleSignOnSessionsEndpoint.toString());
        assertTrue(ticketRegistry.getTickets(ticket -> ticket.getId().equals(tgt.getId()) && !ticket.isExpired()).findAny().isEmpty());
    }

    @Test
    public void verifyOperation() {
        var results = singleSignOnSessionsEndpoint.getSsoSessions(
            SingleSignOnSessionsEndpoint.SsoSessionReportOptions.ALL.getType(), StringUtils.EMPTY,
            0, 1_000);
        assertFalse(results.isEmpty());
        assertTrue(results.containsKey("totalUsageCount"));
        assertTrue(results.containsKey("activeSsoSessions"));
        assertTrue(results.containsKey("totalTicketGrantingTickets"));
        assertTrue(results.containsKey("totalTickets"));
        assertTrue(results.containsKey("totalPrincipals"));
        assertTrue(results.containsKey("totalProxyGrantingTickets"));

        val sessions = (List) results.get("activeSsoSessions");
        assertEquals(1, sessions.size());

        val tgt = Map.class.cast(sessions.get(0))
            .get(SingleSignOnSessionsEndpoint.SsoSessionAttributeKeys.TICKET_GRANTING_TICKET.getAttributeKey()).toString();
        results = singleSignOnSessionsEndpoint.destroySsoSession(tgt, new MockHttpServletRequest(), new MockHttpServletResponse());
        assertFalse(results.isEmpty());
        assertTrue(results.containsKey("status"));
        assertTrue(results.containsKey("ticketGrantingTicket"));

        results = singleSignOnSessionsEndpoint.destroySsoSessions(SingleSignOnSessionsEndpoint.SsoSessionReportOptions.ALL.getType(),
            null, 0, 1_000, new MockHttpServletRequest(), new MockHttpServletResponse());
        assertEquals(1, results.size());
        assertTrue(results.containsKey("status"));
    }

    @Test
    public void verifyProxies() throws Exception {
        val tgt = new MockTicketGrantingTicket("casuser");
        tgt.setProxiedBy(CoreAuthenticationTestUtils.getWebApplicationService());
        ticketRegistry.addTicket(tgt);
        var results = singleSignOnSessionsEndpoint.getSsoSessions(SingleSignOnSessionsEndpoint.SsoSessionReportOptions.ALL.getType(),
            StringUtils.EMPTY, 0, 1_000);
        assertFalse(results.isEmpty());
        results = singleSignOnSessionsEndpoint.getSsoSessions(null, null, 0, 1_000);
        assertFalse(results.isEmpty());
    }

    @Test
    public void verifyDirect() throws Exception {
        val tgt = new MockTicketGrantingTicket("casuser");
        tgt.setProxiedBy(CoreAuthenticationTestUtils.getWebApplicationService());
        ticketRegistry.addTicket(tgt);
        var results = singleSignOnSessionsEndpoint.getSsoSessions(SingleSignOnSessionsEndpoint.SsoSessionReportOptions.DIRECT.getType(),
            StringUtils.EMPTY, 0, 1_000);
        assertFalse(results.isEmpty());
        results = singleSignOnSessionsEndpoint.getSsoSessions(null, null, 0, 1_000);
        assertFalse(results.isEmpty());
    }

    @Test
    public void verifyDeleteFails() throws Exception {
        val registry = mock(TicketRegistry.class);
        when(registry.getTickets(any(Predicate.class))).thenReturn(Stream.of(new MockTicketGrantingTicket("casuser")));
        when(registry.deleteTicket(anyString())).thenThrow(new RuntimeException());

        val results = new SingleSignOnSessionsEndpoint(new DirectObjectProvider<>(registry),
            casProperties, new DirectObjectProvider<>(defaultSingleLogoutRequestExecutor)).destroySsoSessions(
            SingleSignOnSessionsEndpoint.SsoSessionReportOptions.DIRECT.getType(), null,
            0, 1_000,
            new MockHttpServletRequest(), new MockHttpServletResponse());
        assertFalse(results.isEmpty());
    }
}

