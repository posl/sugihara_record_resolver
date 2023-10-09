package org.apereo.cas.ticket;

import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.config.CasCoreAuthenticationConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationPrincipalConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationServiceSelectionStrategyConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationSupportConfiguration;
import org.apereo.cas.config.CasCoreConfiguration;
import org.apereo.cas.config.CasCoreHttpConfiguration;
import org.apereo.cas.config.CasCoreNotificationsConfiguration;
import org.apereo.cas.config.CasCoreServicesConfiguration;
import org.apereo.cas.config.CasCoreTicketCatalogConfiguration;
import org.apereo.cas.config.CasCoreTicketIdGeneratorsConfiguration;
import org.apereo.cas.config.CasCoreTicketsConfiguration;
import org.apereo.cas.config.CasCoreTicketsSerializationConfiguration;
import org.apereo.cas.config.CasCoreUtilConfiguration;
import org.apereo.cas.config.CasCoreWebConfiguration;
import org.apereo.cas.config.CasDefaultServiceTicketIdGeneratorsConfiguration;
import org.apereo.cas.config.CasPersonDirectoryConfiguration;
import org.apereo.cas.config.support.CasWebApplicationServiceFactoryConfiguration;
import org.apereo.cas.logout.config.CasCoreLogoutConfiguration;
import org.apereo.cas.mock.MockTicketGrantingTicket;
import org.apereo.cas.ticket.proxy.ProxyGrantingTicket;
import org.apereo.cas.ticket.proxy.ProxyTicket;
import org.apereo.cas.web.config.CasCookieConfiguration;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link DefaultTicketCatalogTests}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@SpringBootTest(classes = {
    RefreshAutoConfiguration.class,
    CasCoreServicesConfiguration.class,
    CasCoreTicketsConfiguration.class,
    CasCoreTicketsSerializationConfiguration.class,
    CasCoreTicketCatalogConfiguration.class,
    CasDefaultServiceTicketIdGeneratorsConfiguration.class,
    CasCoreTicketIdGeneratorsConfiguration.class,
    CasWebApplicationServiceFactoryConfiguration.class,
    CasCoreUtilConfiguration.class,
    CasCoreWebConfiguration.class,
    CasCoreHttpConfiguration.class,
    CasCookieConfiguration.class,
    CasCoreConfiguration.class,
    CasCoreNotificationsConfiguration.class,
    CasPersonDirectoryConfiguration.class,
    CasCoreAuthenticationConfiguration.class,
    CasCoreAuthenticationServiceSelectionStrategyConfiguration.class,
    CasCoreAuthenticationSupportConfiguration.class,
    CasCoreAuthenticationPrincipalConfiguration.class,
    CasCoreLogoutConfiguration.class
})
@Tag("Tickets")
public class DefaultTicketCatalogTests {
    @Autowired
    @Qualifier(TicketCatalog.BEAN_NAME)
    private TicketCatalog ticketCatalog;

    @Autowired
    @Qualifier(ServiceTicketSessionTrackingPolicy.BEAN_NAME)
    private ServiceTicketSessionTrackingPolicy serviceTicketSessionTrackingPolicy;

    @Test
    public void verifyFindAll() {
        val tickets = ticketCatalog.findAll();
        assertFalse(tickets.isEmpty());
        assertEquals(5, tickets.size());
    }

    @Test
    public void verifyByTicketType() {
        assertTrue(ticketCatalog.findTicketDefinition(TicketGrantingTicket.class).isPresent());
        assertTrue(ticketCatalog.findTicketDefinition(ProxyGrantingTicket.class).isPresent());
        assertTrue(ticketCatalog.findTicketDefinition(ProxyTicket.class).isPresent());
        assertTrue(ticketCatalog.findTicketDefinition(ServiceTicket.class).isPresent());
        assertTrue(ticketCatalog.findTicketDefinition(TransientSessionTicket.class).isPresent());
    }

    @Test
    public void verifyUpdateAndFind() {
        val defn = ticketCatalog.findTicketDefinition(TicketGrantingTicket.class).get();
        ticketCatalog.update(defn);
        assertTrue(ticketCatalog.contains(defn.getPrefix()));
    }

    @Test
    public void verifyContains() {
        val tgt = new MockTicketGrantingTicket("casuser");
        assertTrue(ticketCatalog.contains(tgt.getPrefix()));
        assertNotNull(ticketCatalog.find(tgt));
        assertNotNull(ticketCatalog.find(tgt.getId()));
        assertNotNull(tgt.getClass());
        val st = tgt.grantServiceTicket(CoreAuthenticationTestUtils.getService(), serviceTicketSessionTrackingPolicy);
        assertTrue(ticketCatalog.contains(st.getPrefix()));
        assertNotNull(ticketCatalog.find(st));
        assertNotNull(ticketCatalog.find(st.getId()));
        assertNotNull(st.getClass());
    }
}
