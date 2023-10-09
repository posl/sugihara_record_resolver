package org.apereo.cas.ticket.registry;

import org.apereo.cas.config.HazelcastTicketRegistryConfiguration;
import org.apereo.cas.config.HazelcastTicketRegistryTicketCatalogConfiguration;
import org.apereo.cas.mock.MockTicketGrantingTicket;
import org.apereo.cas.ticket.DefaultTicketDefinition;
import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.TicketCatalog;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.expiration.HardTimeoutExpirationPolicy;

import com.hazelcast.core.HazelcastInstance;
import lombok.Getter;
import lombok.val;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link HazelcastTicketRegistry}.
 *
 * @author Dmitriy Kopylenko
 * @since 4.1.0
 */
@Import({
    HazelcastTicketRegistryConfiguration.class,
    HazelcastTicketRegistryTicketCatalogConfiguration.class,
    BaseTicketRegistryTests.SharedTestConfiguration.class
})
@TestPropertySource(
    properties = {
        "cas.ticket.registry.hazelcast.cluster.network.port-auto-increment=false",
        "cas.ticket.registry.hazelcast.cluster.network.port=5703",
        "cas.ticket.registry.hazelcast.cluster.core.instance-name=testlocalhostinstance"
    })
@Tag("Hazelcast")
@Getter
public class HazelcastTicketRegistryTests extends BaseTicketRegistryTests {
    @Autowired
    @Qualifier(TicketRegistry.BEAN_NAME)
    private TicketRegistry newTicketRegistry;

    @RepeatedTest(1)
    public void verifyBadExpPolicyValue() {
        val instance = mock(HazelcastInstance.class);
        val catalog = mock(TicketCatalog.class);
        try (val registry = new HazelcastTicketRegistry(instance, catalog, 0)) {
            val ticket = new MockTicketGrantingTicket("casuser");
            ticket.setExpirationPolicy(new HardTimeoutExpirationPolicy(-1));
            assertThrows(IllegalArgumentException.class,
                () -> registry.addTicket(ticket));
            assertDoesNotThrow(registry::shutdown);
        }
    }

    @RepeatedTest(1)
    public void verifyBadTicketInCatalog() {
        val ticket = new MockTicketGrantingTicket("casuser");

        val instance = mock(HazelcastInstance.class);
        when(instance.getMap(anyString())).thenThrow(new RuntimeException());

        val catalog = mock(TicketCatalog.class);
        val defn = new DefaultTicketDefinition(ticket.getClass(), TicketGrantingTicket.class, ticket.getPrefix(), 0);
        defn.getProperties().setStorageName("Tickets");
        when(catalog.find(any(Ticket.class))).thenReturn(defn);
        try (val registry = new HazelcastTicketRegistry(instance, catalog, 0)) {
            assertDoesNotThrow(() -> registry.addTicket(ticket));
            assertNull(registry.getTicket(ticket.getId()));
        }
    }

}
