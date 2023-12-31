package org.apereo.cas.ticket.factory;

import org.apereo.cas.configuration.support.Beans;
import org.apereo.cas.services.CasRegisteredService;
import org.apereo.cas.services.DefaultRegisteredServiceTicketGrantingTicketExpirationPolicy;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.TicketGrantingTicketFactory;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.Serial;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link DefaultTicketGrantingTicketFactoryTests}.
 *
 * @author Misagh Moayyed
 * @since 6.3.0
 */
@Tag("Tickets")
public class DefaultTicketGrantingTicketFactoryTests extends BaseTicketFactoryTests {

    @Test
    public void verifyNoExpirationPolicy() {
        val service = RegisteredServiceTestUtils.getService("noExpirationPolicy");
        val factory = (TicketGrantingTicketFactory) this.ticketFactory.get(TicketGrantingTicket.class);
        val tgt = factory.create(RegisteredServiceTestUtils.getAuthentication(), service, TicketGrantingTicket.class);
        val seconds = Beans.newDuration(casProperties.getTicket().getTgt().getPrimary().getMaxTimeToLiveInSeconds()).toSeconds();
        assertEquals(seconds, tgt.getExpirationPolicy().getTimeToLive());
    }

    @Test
    public void verifyBadTicketType() {
        val service = RegisteredServiceTestUtils.getService("noExpirationPolicy");
        val factory = (TicketGrantingTicketFactory) this.ticketFactory.get(TicketGrantingTicket.class);
        assertThrows(ClassCastException.class,
            () -> factory.create(RegisteredServiceTestUtils.getAuthentication(), service, BaseMockTicketGrantingTicket.class));
    }

    @Test
    public void verifyCustomExpirationPolicy() {
        val defaultSvc = RegisteredServiceTestUtils.getRegisteredService("customTgtExpirationPolicy", CasRegisteredService.class);
        defaultSvc.setTicketGrantingTicketExpirationPolicy(
            new DefaultRegisteredServiceTicketGrantingTicketExpirationPolicy(120));
        servicesManager.save(defaultSvc);

        val service = RegisteredServiceTestUtils.getService("customTgtExpirationPolicy");
        val factory = (TicketGrantingTicketFactory) this.ticketFactory.get(TicketGrantingTicket.class);
        val tgt = factory.create(RegisteredServiceTestUtils.getAuthentication(), service, TicketGrantingTicket.class);
        assertEquals(120, tgt.getExpirationPolicy().getTimeToLive());
    }

    abstract static class BaseMockTicketGrantingTicket implements TicketGrantingTicket {
        @Serial
        private static final long serialVersionUID = 6712185629825357896L;
    }

}
