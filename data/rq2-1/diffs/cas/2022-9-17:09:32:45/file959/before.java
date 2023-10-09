package org.apereo.cas.ticket.registry;

import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.config.CasCoreAuthenticationConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationHandlersConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationMetadataConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationPolicyConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationPrincipalConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationServiceSelectionStrategyConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationSupportConfiguration;
import org.apereo.cas.config.CasCoreConfiguration;
import org.apereo.cas.config.CasCoreHttpConfiguration;
import org.apereo.cas.config.CasCoreNotificationsConfiguration;
import org.apereo.cas.config.CasCoreServicesAuthenticationConfiguration;
import org.apereo.cas.config.CasCoreServicesConfiguration;
import org.apereo.cas.config.CasCoreTicketCatalogConfiguration;
import org.apereo.cas.config.CasCoreTicketIdGeneratorsConfiguration;
import org.apereo.cas.config.CasCoreTicketsConfiguration;
import org.apereo.cas.config.CasCoreTicketsSchedulingConfiguration;
import org.apereo.cas.config.CasCoreTicketsSerializationConfiguration;
import org.apereo.cas.config.CasCoreUtilConfiguration;
import org.apereo.cas.config.CasCoreWebConfiguration;
import org.apereo.cas.config.CasPersonDirectoryConfiguration;
import org.apereo.cas.config.support.CasWebApplicationServiceFactoryConfiguration;
import org.apereo.cas.configuration.model.core.util.EncryptionRandomizedSigningJwtCryptographyProperties;
import org.apereo.cas.logout.config.CasCoreLogoutConfiguration;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.ticket.AbstractTicket;
import org.apereo.cas.ticket.AuthenticatedServicesAwareTicketGrantingTicket;
import org.apereo.cas.ticket.ExpirationPolicy;
import org.apereo.cas.ticket.ExpirationPolicyBuilder;
import org.apereo.cas.ticket.InvalidTicketException;
import org.apereo.cas.ticket.ProxyGrantingTicketIssuerTicket;
import org.apereo.cas.ticket.ServiceTicket;
import org.apereo.cas.ticket.ServiceTicketSessionTrackingPolicy;
import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.TicketFactory;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.TicketGrantingTicketImpl;
import org.apereo.cas.ticket.TransientSessionTicket;
import org.apereo.cas.ticket.TransientSessionTicketImpl;
import org.apereo.cas.ticket.expiration.AlwaysExpiresExpirationPolicy;
import org.apereo.cas.ticket.expiration.NeverExpiresExpirationPolicy;
import org.apereo.cas.ticket.expiration.TicketGrantingTicketExpirationPolicy;
import org.apereo.cas.ticket.expiration.TimeoutExpirationPolicy;
import org.apereo.cas.ticket.proxy.ProxyGrantingTicket;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.CoreTicketUtils;
import org.apereo.cas.util.DefaultUniqueTicketIdGenerator;
import org.apereo.cas.util.ProxyGrantingTicketIdGenerator;
import org.apereo.cas.util.ServiceTicketIdGenerator;
import org.apereo.cas.util.TicketGrantingTicketIdGenerator;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.util.function.FunctionUtils;
import org.apereo.cas.web.config.CasCookieConfiguration;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Unchecked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * This is {@link BaseTicketRegistryTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@Slf4j
@SpringBootTest(classes = BaseTicketRegistryTests.SharedTestConfiguration.class,
    properties = {
        "cas.ticket.tgt.core.only-track-most-recent-session=false",
        "cas.ticket.registry.cleaner.schedule.enabled=false"
    })
public abstract class BaseTicketRegistryTests {

    private static final int TICKETS_IN_REGISTRY = 1;

    private static final String TICKET_SHOULD_BE_NULL_USE_ENCRYPTION = "Ticket should be null. useEncryption[";

    @Autowired
    @Qualifier(TicketFactory.BEAN_NAME)
    protected TicketFactory ticketFactory;

    @Autowired
    @Qualifier(ServiceTicketSessionTrackingPolicy.BEAN_NAME)
    protected ServiceTicketSessionTrackingPolicy serviceTicketSessionTrackingPolicy;

    protected boolean useEncryption;

    protected String ticketGrantingTicketId;

    protected String serviceTicketId;

    protected String transientSessionTicketId;

    protected String proxyGrantingTicketId;

    private TicketRegistry ticketRegistry;

    protected static ExpirationPolicyBuilder neverExpiresExpirationPolicyBuilder() {
        return new ExpirationPolicyBuilder() {
            private static final long serialVersionUID = -9043565995104313970L;

            @Override
            public ExpirationPolicy buildTicketExpirationPolicy() {
                return NeverExpiresExpirationPolicy.INSTANCE;
            }
        };
    }

    @BeforeEach
    public void initialize(final TestInfo info, final RepetitionInfo repetitionInfo) {
        this.ticketGrantingTicketId = new TicketGrantingTicketIdGenerator(10, StringUtils.EMPTY)
            .getNewTicketId(TicketGrantingTicket.PREFIX);
        this.serviceTicketId = new ServiceTicketIdGenerator(10, StringUtils.EMPTY)
            .getNewTicketId(ServiceTicket.PREFIX);
        this.proxyGrantingTicketId = new ProxyGrantingTicketIdGenerator(10, StringUtils.EMPTY)
            .getNewTicketId(ProxyGrantingTicket.PROXY_GRANTING_TICKET_PREFIX);
        this.transientSessionTicketId = new DefaultUniqueTicketIdGenerator().getNewTicketId(TransientSessionTicket.PREFIX);

        if (info.getTags().contains("TicketRegistryTestWithEncryption")) {
            useEncryption = true;
        } else if (info.getTags().contains("TicketRegistryTestWithoutEncryption")) {
            useEncryption = false;
        } else {
            useEncryption = repetitionInfo.getTotalRepetitions() == 2 && repetitionInfo.getCurrentRepetition() == 2;
        }
        ticketRegistry = this.getNewTicketRegistry();
        if (ticketRegistry != null) {
            ticketRegistry.deleteAll();
            setUpEncryption();
        }
    }

    @RepeatedTest(2)
    public void verifyAddTicketWithStream() throws Exception {
        val originalAuthn = CoreAuthenticationTestUtils.getAuthentication();
        val s1 = Stream.of(new TicketGrantingTicketImpl(ticketGrantingTicketId,
            originalAuthn, NeverExpiresExpirationPolicy.INSTANCE));
        ticketRegistry.addTicket(s1);
        val tgt = ticketRegistry.getTicket(ticketGrantingTicketId, TicketGrantingTicket.class);
        assertNotNull(tgt);
    }

    @RepeatedTest(2)
    public void verifyUnableToAddExpiredTicket() throws Exception {
        val originalAuthn = CoreAuthenticationTestUtils.getAuthentication();
        val s1 = Stream.of(new TicketGrantingTicketImpl(ticketGrantingTicketId,
            originalAuthn, AlwaysExpiresExpirationPolicy.INSTANCE));
        ticketRegistry.addTicket(s1);
        assertThrows(InvalidTicketException.class, () -> ticketRegistry.getTicket(ticketGrantingTicketId, TicketGrantingTicket.class));
    }

    @RepeatedTest(2)
    public void verifyAddTicketToCache() throws Exception {
        val originalAuthn = CoreAuthenticationTestUtils.getAuthentication();
        ticketRegistry.addTicket(new TicketGrantingTicketImpl(ticketGrantingTicketId,
            originalAuthn,
            NeverExpiresExpirationPolicy.INSTANCE));
        val tgt = ticketRegistry.getTicket(ticketGrantingTicketId, TicketGrantingTicket.class);
        assertNotNull(tgt);
        val authentication = tgt.getAuthentication();
        assertNotNull(authentication);
        assertNotNull(authentication.getSuccesses());
        assertNotNull(authentication.getWarnings());
        assertNotNull(authentication.getFailures());
    }

    @RepeatedTest(2)
    public void verifyDeleteExpiredTicketById() throws Exception {
        val expirationPolicy = new TicketGrantingTicketExpirationPolicy(42, 23);
        val ticketGrantingTicket = new TicketGrantingTicketImpl(ticketGrantingTicketId,
            CoreAuthenticationTestUtils.getAuthentication(), expirationPolicy);
        expirationPolicy.setClock(Clock.fixed(ticketGrantingTicket.getCreationTime().toInstant(), ZoneOffset.UTC));
        assertFalse(ticketGrantingTicket.isExpired());
        getNewTicketRegistry().addTicket(ticketGrantingTicket);
        ticketGrantingTicket.markTicketExpired();
        assertTrue(ticketGrantingTicket.isExpired());
        val deletedTicketCount = getNewTicketRegistry().deleteTicket(ticketGrantingTicket.getId());
        assertTrue(deletedTicketCount <= 1);
    }

    @RepeatedTest(2)
    public void verifyTicketWithTimeoutPolicy() throws Exception {
        val originalAuthn = CoreAuthenticationTestUtils.getAuthentication();
        ticketRegistry.addTicket(new TicketGrantingTicketImpl(ticketGrantingTicketId,
            originalAuthn,
            TimeoutExpirationPolicy.builder().timeToKillInSeconds(5).build()));
        val tgt = ticketRegistry.getTicket(ticketGrantingTicketId, TicketGrantingTicket.class);
        assertNotNull(tgt);
    }

    @RepeatedTest(2)
    public void verifyGetNullTicket() {
        assertThrows(InvalidTicketException.class, () -> ticketRegistry.getTicket(null, TicketGrantingTicket.class),
            () -> TICKET_SHOULD_BE_NULL_USE_ENCRYPTION + useEncryption + ']');
    }

    @RepeatedTest(2)
    public void verifyGetNonExistingTicket() {
        assertThrows(InvalidTicketException.class, () -> ticketRegistry.getTicket("unknown-ticket", TicketGrantingTicket.class),
            () -> TICKET_SHOULD_BE_NULL_USE_ENCRYPTION + useEncryption + ']');
    }

    @RepeatedTest(2)
    public void verifyGetExistingTicketWithProperClass() throws Exception {
        ticketRegistry.addTicket(new TicketGrantingTicketImpl(ticketGrantingTicketId,
            CoreAuthenticationTestUtils.getAuthentication(),
            NeverExpiresExpirationPolicy.INSTANCE));
        val ticket = ticketRegistry.getTicket(ticketGrantingTicketId, TicketGrantingTicket.class);
        assertNotNull(ticket, () -> "Ticket is null. useEncryption[" + useEncryption + ']');
        assertEquals(ticketGrantingTicketId, ticket.getId(), () -> "Ticket IDs don't match. useEncryption[" + useEncryption + ']');
    }

    @RepeatedTest(2)
    public void verifyCountSessionsPerUser() throws Exception {
        assumeTrue(isIterableRegistry());
        val id = UUID.randomUUID().toString();
        ticketRegistry.addTicket(new TicketGrantingTicketImpl(ticketGrantingTicketId,
            CoreAuthenticationTestUtils.getAuthentication(id),
            NeverExpiresExpirationPolicy.INSTANCE));
        val count = ticketRegistry.countSessionsFor(id);
        assertTrue(count > 0);
    }

    @RepeatedTest(2)
    @Transactional
    public void verifyGetSsoSessionsPerUser() throws Exception {
        assumeTrue(isIterableRegistry());
        val id = UUID.randomUUID().toString();
        for (var i = 0; i < 5; i++) {
            val tgtId = new TicketGrantingTicketIdGenerator(10, StringUtils.EMPTY)
                .getNewTicketId(TicketGrantingTicket.PREFIX);
            ticketRegistry.addTicket(new TicketGrantingTicketImpl(tgtId,
                CoreAuthenticationTestUtils.getAuthentication(id),
                NeverExpiresExpirationPolicy.INSTANCE));
        }
        try (val results = ticketRegistry.getSessionsFor(id)) {
            assertEquals(5, results.count());
        }
    }

    @RepeatedTest(2)
    public void verifyGetExistingTicketWithImproperClass() {
        FunctionUtils.doAndRetry(callback -> {
            ticketRegistry.addTicket(new TicketGrantingTicketImpl(ticketGrantingTicketId,
                CoreAuthenticationTestUtils.getAuthentication(),
                NeverExpiresExpirationPolicy.INSTANCE));

            assertThrows(ClassCastException.class,
                () -> ticketRegistry.getTicket(ticketGrantingTicketId, ServiceTicket.class),
                () -> "Should throw ClassCastException. useEncryption[" + useEncryption + ']');
            return null;
        });
    }

    @RepeatedTest(2)
    public void verifyGetNullTicketWithoutClass() {
        assertNull(ticketRegistry.getTicket(null), () -> TICKET_SHOULD_BE_NULL_USE_ENCRYPTION + useEncryption + ']');
    }

    @RepeatedTest(2)
    public void verifyGetNonExistingTicketWithoutClass() {
        assertNull(ticketRegistry.getTicket("FALALALALALAL"), () -> TICKET_SHOULD_BE_NULL_USE_ENCRYPTION + useEncryption + ']');
    }

    @RepeatedTest(2)
    public void verifyGetExistingTicket() throws Exception {
        ticketRegistry.addTicket(new TicketGrantingTicketImpl(ticketGrantingTicketId,
            CoreAuthenticationTestUtils.getAuthentication(),
            NeverExpiresExpirationPolicy.INSTANCE));
        val ticket = ticketRegistry.getTicket(ticketGrantingTicketId);
        assertNotNull(ticket, () -> "Ticket is null. useEncryption[" + useEncryption + ']');
        assertEquals(ticketGrantingTicketId, ticket.getId(), () -> "Ticket IDs don't match. useEncryption[" + useEncryption + ']');
    }

    @RepeatedTest(2)
    public void verifyAddAndUpdateTicket() throws Exception {
        val tgt = new TicketGrantingTicketImpl(
            ticketGrantingTicketId,
            CoreAuthenticationTestUtils.getAuthentication(),
            NeverExpiresExpirationPolicy.INSTANCE);
        ticketRegistry.addTicket(tgt);

        await().untilAsserted(() -> assertNotNull(ticketRegistry.getTicket(tgt.getId(), TicketGrantingTicket.class)));

        val found = ticketRegistry.getTicket(tgt.getId(), TicketGrantingTicket.class);
        assertNotNull(found, () -> "Ticket is null. useEncryption[" + useEncryption + ']');

        assertTrue(found instanceof AuthenticatedServicesAwareTicketGrantingTicket);
        var services = ((AuthenticatedServicesAwareTicketGrantingTicket) found).getServices();
        assertTrue(services.isEmpty(), () -> "Ticket services should be empty. useEncryption[" + useEncryption + ']');

        tgt.grantServiceTicket("ST1", RegisteredServiceTestUtils.getService("TGT_UPDATE_TEST"),
            NeverExpiresExpirationPolicy.INSTANCE, false, serviceTicketSessionTrackingPolicy);
        ticketRegistry.updateTicket(tgt);
        val tgtResult = ticketRegistry.getTicket(tgt.getId(), TicketGrantingTicket.class);
        assertTrue(tgtResult instanceof AuthenticatedServicesAwareTicketGrantingTicket);
        services = ((AuthenticatedServicesAwareTicketGrantingTicket) tgtResult).getServices();
        assertEquals(Collections.singleton("ST1"), services.keySet());
    }

    @RepeatedTest(2)
    public void verifyDeleteAllExistingTickets() throws Exception {
        assumeTrue(isIterableRegistry());
        for (var i = 0; i < TICKETS_IN_REGISTRY; i++) {
            ticketRegistry.addTicket(new TicketGrantingTicketImpl(ticketGrantingTicketId + i,
                CoreAuthenticationTestUtils.getAuthentication(),
                NeverExpiresExpirationPolicy.INSTANCE));
        }
        val actual = ticketRegistry.deleteAll();
        if (actual <= 0) {
            LOGGER.warn("Ticket registry does not support reporting count of deleted rows");
        } else {
            assertEquals(TICKETS_IN_REGISTRY, actual, () -> "Wrong ticket count. useEncryption[" + useEncryption + ']');
        }
    }

    @RepeatedTest(2)
    public void verifyDeleteExistingTicket() throws Exception {
        ticketRegistry.addTicket(new TicketGrantingTicketImpl(ticketGrantingTicketId,
            CoreAuthenticationTestUtils.getAuthentication(),
            NeverExpiresExpirationPolicy.INSTANCE));
        assertSame(1, ticketRegistry.deleteTicket(ticketGrantingTicketId), () -> "Wrong ticket count. useEncryption[" + useEncryption + ']');
        assertNull(ticketRegistry.getTicket(ticketGrantingTicketId), () -> TICKET_SHOULD_BE_NULL_USE_ENCRYPTION + useEncryption + ']');
    }

    @RepeatedTest(2)
    public void verifyTransientSessionTickets() throws Exception {
        ticketRegistry.addTicket(new TransientSessionTicketImpl(transientSessionTicketId, NeverExpiresExpirationPolicy.INSTANCE,
            RegisteredServiceTestUtils.getService(), CollectionUtils.wrap("key", "value")));
        assertSame(1, ticketRegistry.deleteTicket(transientSessionTicketId), () -> "Wrong ticket count. useEncryption[" + useEncryption + ']');
        assertNull(ticketRegistry.getTicket(transientSessionTicketId), () -> TICKET_SHOULD_BE_NULL_USE_ENCRYPTION + useEncryption + ']');
    }

    @RepeatedTest(2)
    public void verifyDeleteNonExistingTicket() throws Exception {
        ticketRegistry.addTicket(new TicketGrantingTicketImpl(ticketGrantingTicketId,
            CoreAuthenticationTestUtils.getAuthentication(),
            NeverExpiresExpirationPolicy.INSTANCE));
        val ticketId = ticketGrantingTicketId + "NON-EXISTING-SUFFIX";
        ticketRegistry.deleteTicket(ticketId);
        assertEquals(0, ticketRegistry.getTickets(ticket -> ticket.getId().equals(ticketId)).count());
    }

    @RepeatedTest(2)
    public void verifyDeleteNullTicket() throws Exception {
        ticketRegistry.addTicket(new TicketGrantingTicketImpl(ticketGrantingTicketId,
            CoreAuthenticationTestUtils.getAuthentication(),
            NeverExpiresExpirationPolicy.INSTANCE));
        assertNotEquals(1, ticketRegistry.deleteTicket(StringUtils.EMPTY), "Ticket was deleted.");
    }

    @RepeatedTest(2)
    public void verifyGetTicketsIsZero() {
        ticketRegistry.deleteAll();
        assertEquals(0, ticketRegistry.getTickets().size(), "The size of the empty registry is not zero.");
    }

    @RepeatedTest(2)
    public void verifyGetTicketsFromRegistryEqualToTicketsAdded() throws Exception {
        assumeTrue(isIterableRegistry());
        val tickets = new ArrayList<Ticket>();

        for (var i = 0; i < TICKETS_IN_REGISTRY; i++) {
            val ticketGrantingTicket = new TicketGrantingTicketImpl(ticketGrantingTicketId + '-' + i,
                CoreAuthenticationTestUtils.getAuthentication(), NeverExpiresExpirationPolicy.INSTANCE);
            val st = ticketGrantingTicket.grantServiceTicket("ST-" + i,
                RegisteredServiceTestUtils.getService(),
                NeverExpiresExpirationPolicy.INSTANCE, false, serviceTicketSessionTrackingPolicy);
            tickets.add(ticketGrantingTicket);
            tickets.add(st);
            ticketRegistry.addTicket(ticketGrantingTicket);
            ticketRegistry.addTicket(st);
        }

        val ticketRegistryTickets = ticketRegistry.getTickets();
        assertEquals(tickets.size(), ticketRegistryTickets.size(), "The size of the registry is not the same as the collection.");


        tickets.stream().filter(ticket -> !ticketRegistryTickets.contains(ticket))
            .forEach(ticket -> {
                throw new AssertionError("Ticket " + ticket + " was not found in retrieval of collection of all tickets.");
            });
    }

    @RepeatedTest(1)
    @Tag("DisableTicketRegistryTestWithEncryption")
    public void verifyTicketCountsEqualToTicketsAdded() {
        assumeTrue(isIterableRegistry());
        val tgts = new ArrayList<Ticket>();
        val sts = new ArrayList<Ticket>();

        FunctionUtils.doAndRetry(callback -> {
            for (var i = 0; i < TICKETS_IN_REGISTRY; i++) {
                val auth = CoreAuthenticationTestUtils.getAuthentication();
                val service = RegisteredServiceTestUtils.getService();
                val ticketGrantingTicket = new TicketGrantingTicketImpl(TicketGrantingTicket.PREFIX + '-' + i,
                    auth, NeverExpiresExpirationPolicy.INSTANCE);
                val st = ticketGrantingTicket.grantServiceTicket("ST-" + i,
                    service, NeverExpiresExpirationPolicy.INSTANCE, false, serviceTicketSessionTrackingPolicy);
                tgts.add(ticketGrantingTicket);
                sts.add(st);
                ticketRegistry.addTicket(ticketGrantingTicket);
                await().untilAsserted(() -> assertNotNull(ticketRegistry.getTicket(ticketGrantingTicket.getId()) != null));
                ticketRegistry.addTicket(st);
                await().untilAsserted(() -> assertNotNull(ticketRegistry.getTicket(st.getId()) != null));
            }
            await().untilAsserted(() -> {
                val sessionCount = ticketRegistry.sessionCount();
                assertEquals(tgts.size(), ticketRegistry.sessionCount(),
                    () -> "The sessionCount " + sessionCount + " is not the same as the collection " + tgts.size());
            });

            await().untilAsserted(() -> {
                val ticketCount = this.ticketRegistry.serviceTicketCount();
                assertEquals(sts.size(), ticketCount,
                    () -> "The serviceTicketCount " + ticketCount + " is not the same as the collection " + sts.size());
            });

            return null;
        });
    }

    @RepeatedTest(2)
    public void verifyDeleteTicketWithChildren() throws Exception {
        ticketRegistry.addTicket(new TicketGrantingTicketImpl(ticketGrantingTicketId + '1', CoreAuthenticationTestUtils.getAuthentication(),
            NeverExpiresExpirationPolicy.INSTANCE));
        val tgt = ticketRegistry.getTicket(ticketGrantingTicketId + '1', TicketGrantingTicket.class);

        val service = RegisteredServiceTestUtils.getService("TGT_DELETE_TEST");

        val st1 = tgt.grantServiceTicket("ST-11", service,
            NeverExpiresExpirationPolicy.INSTANCE, false, serviceTicketSessionTrackingPolicy);
        val st2 = tgt.grantServiceTicket("ST-21", service,
            NeverExpiresExpirationPolicy.INSTANCE, false, serviceTicketSessionTrackingPolicy);
        val st3 = tgt.grantServiceTicket("ST-31", service,
            NeverExpiresExpirationPolicy.INSTANCE, false, serviceTicketSessionTrackingPolicy);

        ticketRegistry.addTicket(st1);
        ticketRegistry.addTicket(st2);
        ticketRegistry.addTicket(st3);

        assertNotNull(ticketRegistry.getTicket(ticketGrantingTicketId + '1', TicketGrantingTicket.class));
        assertNotNull(ticketRegistry.getTicket("ST-11", ServiceTicket.class));
        assertNotNull(ticketRegistry.getTicket("ST-21", ServiceTicket.class));
        assertNotNull(ticketRegistry.getTicket("ST-31", ServiceTicket.class));

        ticketRegistry.updateTicket(tgt);
        assertSame(4, ticketRegistry.deleteTicket(tgt.getId()));

        assertThrows(InvalidTicketException.class, () -> ticketRegistry.getTicket(ticketGrantingTicketId + '1', TicketGrantingTicket.class));
        assertThrows(InvalidTicketException.class, () -> ticketRegistry.getTicket("ST-11", ServiceTicket.class));
        assertThrows(InvalidTicketException.class, () -> ticketRegistry.getTicket("ST-21", ServiceTicket.class));
        assertThrows(InvalidTicketException.class, () -> ticketRegistry.getTicket("ST-31", ServiceTicket.class));
    }

    @RepeatedTest(2)
    public void verifyWriteGetDelete() throws Exception {
        val ticket = new TicketGrantingTicketImpl(ticketGrantingTicketId,
            CoreAuthenticationTestUtils.getAuthentication(),
            NeverExpiresExpirationPolicy.INSTANCE);
        ticketRegistry.addTicket(ticket);
        val ticketFromRegistry = ticketRegistry.getTicket(ticketGrantingTicketId);
        assertNotNull(ticketFromRegistry);
        assertEquals(ticketGrantingTicketId, ticketFromRegistry.getId());
        ticketRegistry.deleteTicket(ticketGrantingTicketId);
        assertNull(ticketRegistry.getTicket(ticketGrantingTicketId));
    }

    @RepeatedTest(2)
    public void verifyExpiration() throws Exception {
        val authn = CoreAuthenticationTestUtils.getAuthentication();
        LOGGER.trace("Adding ticket [{}]", ticketGrantingTicketId);
        ticketRegistry.addTicket(new TicketGrantingTicketImpl(ticketGrantingTicketId, authn, NeverExpiresExpirationPolicy.INSTANCE));
        LOGGER.trace("Getting ticket [{}]", ticketGrantingTicketId);
        val tgt = ticketRegistry.getTicket(ticketGrantingTicketId, TicketGrantingTicket.class);
        assertNotNull(tgt, () -> "Ticket-granting ticket " + ticketGrantingTicketId + " cannot be fetched");
        val service = RegisteredServiceTestUtils.getService("TGT_DELETE_TEST");
        LOGGER.trace("Granting service ticket [{}]", serviceTicketId);
        val ticket = (AbstractTicket) tgt.grantServiceTicket(serviceTicketId, service,
            NeverExpiresExpirationPolicy.INSTANCE, false, serviceTicketSessionTrackingPolicy);
        assertNotNull(ticket, "Service ticket cannot be null");
        ticket.setExpirationPolicy(AlwaysExpiresExpirationPolicy.INSTANCE);
        ticketRegistry.addTicket(ticket);
        ticketRegistry.updateTicket(tgt);
        assertThrows(InvalidTicketException.class, () -> ticketRegistry.getTicket(serviceTicketId, ServiceTicket.class));
    }

    @RepeatedTest(2)
    public void verifyExpiredTicket() throws Exception {
        val authn = CoreAuthenticationTestUtils.getAuthentication();
        ticketRegistry.addTicket(new TicketGrantingTicketImpl(ticketGrantingTicketId, authn, AlwaysExpiresExpirationPolicy.INSTANCE));
        assertThrows(InvalidTicketException.class, () -> ticketRegistry.getTicket(ticketGrantingTicketId, TicketGrantingTicket.class));
    }

    @RepeatedTest(2)
    public void verifyDeleteTicketWithPGT() throws Exception {
        val authentication = CoreAuthenticationTestUtils.getAuthentication();
        ticketRegistry.addTicket(new TicketGrantingTicketImpl(ticketGrantingTicketId,
            authentication, NeverExpiresExpirationPolicy.INSTANCE));
        val tgt = ticketRegistry.getTicket(ticketGrantingTicketId, TicketGrantingTicket.class);

        val service = RegisteredServiceTestUtils.getService("TGT_DELETE_TEST");

        val st1 = (ProxyGrantingTicketIssuerTicket) tgt.grantServiceTicket(serviceTicketId,
            service, NeverExpiresExpirationPolicy.INSTANCE, false, serviceTicketSessionTrackingPolicy);
        ticketRegistry.addTicket(st1);
        ticketRegistry.updateTicket(tgt);

        assertNotNull(ticketRegistry.getTicket(ticketGrantingTicketId, TicketGrantingTicket.class));
        assertNotNull(ticketRegistry.getTicket(serviceTicketId, ServiceTicket.class));

        val pgt = st1.grantProxyGrantingTicket(proxyGrantingTicketId, authentication, NeverExpiresExpirationPolicy.INSTANCE);
        ticketRegistry.addTicket(pgt);
        ticketRegistry.updateTicket(tgt);
        ticketRegistry.updateTicket(st1);
        assertEquals(pgt.getTicketGrantingTicket(), tgt);
        assertNotNull(ticketRegistry.getTicket(proxyGrantingTicketId, ProxyGrantingTicket.class));
        assertEquals(authentication, pgt.getAuthentication());
        assertNotNull(ticketRegistry.getTicket(serviceTicketId, ServiceTicket.class));

        await().untilAsserted(() -> assertTrue(ticketRegistry.deleteTicket(tgt.getId()) > 0));

        assertThrows(InvalidTicketException.class, () -> ticketRegistry.getTicket(ticketGrantingTicketId, TicketGrantingTicket.class));
        assertThrows(InvalidTicketException.class, () -> ticketRegistry.getTicket(serviceTicketId, ServiceTicket.class));
        assertThrows(InvalidTicketException.class, () -> ticketRegistry.getTicket(proxyGrantingTicketId, ProxyGrantingTicket.class));
    }

    @RepeatedTest(2)
    public void verifyDeleteTicketsWithMultiplePGTs() {
        FunctionUtils.doAndRetry(callback -> {
            val a = CoreAuthenticationTestUtils.getAuthentication();
            ticketRegistry.addTicket(new TicketGrantingTicketImpl(ticketGrantingTicketId, a, NeverExpiresExpirationPolicy.INSTANCE));
            val tgt = ticketRegistry.getTicket(ticketGrantingTicketId, TicketGrantingTicket.class);
            assertNotNull(tgt, "Ticket-granting ticket must not be null");
            val service = RegisteredServiceTestUtils.getService("TGT_DELETE_TEST");
            IntStream.range(1, 5).forEach(Unchecked.intConsumer(i -> {
                val st = (ProxyGrantingTicketIssuerTicket) tgt.grantServiceTicket(serviceTicketId + '-' + i, service,
                    NeverExpiresExpirationPolicy.INSTANCE, false, serviceTicketSessionTrackingPolicy);
                ticketRegistry.addTicket(st);
                ticketRegistry.updateTicket(tgt);

                val pgt = st.grantProxyGrantingTicket(proxyGrantingTicketId + '-' + i, a, NeverExpiresExpirationPolicy.INSTANCE);
                ticketRegistry.addTicket(pgt);
                ticketRegistry.updateTicket(tgt);
                ticketRegistry.updateTicket(st);
            }));

            val count = ticketRegistry.deleteTicket(ticketGrantingTicketId);
            assertEquals(9, count);
            return null;
        });
    }

    protected abstract TicketRegistry getNewTicketRegistry();

    /**
     * Determine whether the tested registry is able to iterate its tickets.
     */
    protected boolean isIterableRegistry() {
        return true;
    }

    @ImportAutoConfiguration(RefreshAutoConfiguration.class)
    @SpringBootConfiguration
    @Import({
        CasCoreHttpConfiguration.class,
        CasCoreTicketsConfiguration.class,
        CasCoreTicketCatalogConfiguration.class,
        CasCoreTicketIdGeneratorsConfiguration.class,
        CasCoreTicketsSchedulingConfiguration.class,
        CasCoreTicketsSerializationConfiguration.class,
        CasCoreUtilConfiguration.class,
        CasPersonDirectoryConfiguration.class,
        CasCoreLogoutConfiguration.class,
        CasCoreAuthenticationConfiguration.class,
        CasCoreServicesAuthenticationConfiguration.class,
        CasCoreAuthenticationPrincipalConfiguration.class,
        CasCoreAuthenticationPolicyConfiguration.class,
        CasCoreAuthenticationMetadataConfiguration.class,
        CasCoreAuthenticationSupportConfiguration.class,
        CasCoreAuthenticationHandlersConfiguration.class,
        CasCoreConfiguration.class,
        CasCookieConfiguration.class,
        CasCoreAuthenticationServiceSelectionStrategyConfiguration.class,
        CasCoreServicesConfiguration.class,
        CasCoreWebConfiguration.class,
        CasCoreNotificationsConfiguration.class,
        CasWebApplicationServiceFactoryConfiguration.class
    })
    static class SharedTestConfiguration {
    }

    private void setUpEncryption() {
        var registry = (AbstractTicketRegistry) AopTestUtils.getTargetObject(ticketRegistry);
        if (this.useEncryption) {
            val cipher = CoreTicketUtils.newTicketRegistryCipherExecutor(
                new EncryptionRandomizedSigningJwtCryptographyProperties(), "[tests]");
            registry.setCipherExecutor(cipher);
        } else {
            registry.setCipherExecutor(CipherExecutor.noOp());
        }
    }
}
