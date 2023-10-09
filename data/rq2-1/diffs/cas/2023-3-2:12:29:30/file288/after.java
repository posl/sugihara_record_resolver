package org.apereo.cas.ticket.registry;

import org.apereo.cas.logout.LogoutManager;
import org.apereo.cas.mock.MockTicketGrantingTicket;
import org.apereo.cas.ticket.DefaultTicketCatalog;
import org.apereo.cas.ticket.expiration.HardTimeoutExpirationPolicy;
import org.apereo.cas.ticket.serialization.TicketSerializationManager;
import org.apereo.cas.util.lock.LockRepository;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link DefaultTicketRegistryCleanerTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@Tag("Tickets")
@SpringBootTest(classes = BaseTicketRegistryTests.SharedTestConfiguration.class)
public class DefaultTicketRegistryCleanerTests {

    @Test
    public void verifyAction() throws Exception {
        val logoutManager = mock(LogoutManager.class);
        val ticketRegistry = new DefaultTicketRegistry(mock(TicketSerializationManager.class), new DefaultTicketCatalog());
        val tgt = new MockTicketGrantingTicket("casuser");
        tgt.setExpirationPolicy(new HardTimeoutExpirationPolicy(1));
        ticketRegistry.addTicket(tgt);
        assertEquals(1, ticketRegistry.getTickets().size());
        val cleaner = new DefaultTicketRegistryCleaner(LockRepository.noOp(), logoutManager, ticketRegistry);
        tgt.markTicketExpired();
        cleaner.clean();
        assertEquals(0, ticketRegistry.sessionCount());
    }


    @Test
    public void verifyCleanFail() {
        val logoutManager = mock(LogoutManager.class);
        val ticketRegistry = mock(TicketRegistry.class);
        when(ticketRegistry.stream()).thenThrow(IllegalArgumentException.class);
        val c = new DefaultTicketRegistryCleaner(LockRepository.noOp(), logoutManager, ticketRegistry);
        assertEquals(0, c.clean());
    }

    @Test
    public void verifyNoCleaner() {
        val logoutManager = mock(LogoutManager.class);
        val ticketRegistry = new DefaultTicketRegistry(mock(TicketSerializationManager.class), new DefaultTicketCatalog());
        val cleaner = new DefaultTicketRegistryCleaner(LockRepository.noOp(), logoutManager, ticketRegistry) {
            @Override
            protected boolean isCleanerSupported() {
                return false;
            }
        };
        assertEquals(0, cleaner.clean());
    }
}
