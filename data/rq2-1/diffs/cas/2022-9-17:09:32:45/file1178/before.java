package org.apereo.cas.ticket.registry;

import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.TicketCatalog;
import org.apereo.cas.ticket.TicketDefinition;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.util.LoggingUtils;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Hazelcast-based implementation of a {@link TicketRegistry}.
 * <p>This implementation just wraps the Hazelcast's {@link IMap}
 * which is an extension of the standard Java's {@code ConcurrentMap}.</p>
 * <p>The heavy lifting of distributed data partitioning, network cluster discovery and
 * join, data replication, etc. is done by Hazelcast's Map implementation.</p>
 *
 * @author Dmitriy Kopylenko
 * @author Jonathan Johnson
 * @since 4.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class HazelcastTicketRegistry extends AbstractTicketRegistry implements AutoCloseable, DisposableBean {
    private final HazelcastInstance hazelcastInstance;

    private final TicketCatalog ticketCatalog;

    private final long pageSize;

    @Override
    public Ticket updateTicket(final Ticket ticket) throws Exception {
        addTicket(ticket);
        return ticket;
    }

    @Override
    public void addTicketInternal(final Ticket ticket) throws Exception {
        val ttl = ticket.getExpirationPolicy().getTimeToLive();
        if (ttl < 0) {
            throw new IllegalArgumentException("The expiration policy of ticket " + ticket.getId() + " is set to use a negative ttl");
        }

        LOGGER.debug("Adding ticket [{}] with ttl [{}s]", ticket.getId(), ttl);
        val encTicket = encodeTicket(ticket);

        val metadata = this.ticketCatalog.find(ticket);
        val ticketMap = getTicketMapInstanceByMetadata(metadata);
        if (ticketMap != null) {
            val holder = HazelcastTicketHolder.builder()
                .id(encTicket.getId())
                .type(metadata.getImplementationClass().getName())
                .principal(encodeTicketId(getPrincipalIdFrom(ticket)))
                .timeToLive(ttl)
                .ticket(encTicket)
                .build();
            ticketMap.set(encTicket.getId(), holder, ttl, TimeUnit.SECONDS);
            LOGGER.debug("Added ticket [{}] with ttl [{}s]", encTicket.getId(), ttl);
        } else {
            LOGGER.warn("Unable to locate ticket map for ticket metadata [{}]", metadata);
        }
    }

    @Override
    public Ticket getTicket(final String ticketId, final Predicate<Ticket> predicate) {
        val encTicketId = encodeTicketId(ticketId);
        if (StringUtils.isBlank(encTicketId)) {
            return null;
        }
        val metadata = this.ticketCatalog.find(ticketId);
        if (metadata != null) {
            val map = getTicketMapInstanceByMetadata(metadata);
            if (map != null) {
                val ticketHolder = map.get(encTicketId);
                if (ticketHolder != null && ticketHolder.getTicket() != null) {
                    val result = decodeTicket(ticketHolder.getTicket());
                    if (predicate != null && predicate.test(result)) {
                        return result;
                    }
                }
                return null;
            } else {
                LOGGER.error("Unable to locate ticket map for ticket definition [{}]", metadata);
            }
        }
        LOGGER.warn("No ticket definition could be found in the catalog to match [{}]", ticketId);
        return null;
    }

    @Override
    public long deleteSingleTicket(final String ticketIdToDelete) {
        val encTicketId = encodeTicketId(ticketIdToDelete);
        val metadata = this.ticketCatalog.find(ticketIdToDelete);
        val map = getTicketMapInstanceByMetadata(metadata);
        return map != null && map.remove(encTicketId) != null ? 1 : 0;
    }

    @Override
    public long deleteAll() {
        return this.ticketCatalog.findAll()
            .stream()
            .map(this::getTicketMapInstanceByMetadata)
            .filter(Objects::nonNull)
            .mapToInt(instance -> {
                val size = instance.size();
                instance.evictAll();
                instance.clear();
                return size;
            })
            .sum();
    }

    @Override
    public Collection<? extends Ticket> getTickets() {
        return this.ticketCatalog.findAll()
            .stream()
            .map(metadata -> getTicketMapInstanceByMetadata(metadata).values())
            .flatMap(tickets -> {
                if (pageSize > 0) {
                    return tickets.stream()
                        .limit(pageSize)
                        .map(HazelcastTicketHolder::getTicket)
                        .collect(Collectors.toList())
                        .stream();
                }
                return tickets
                    .stream()
                    .map(HazelcastTicketHolder::getTicket);
            })
            .map(this::decodeTicket)
            .collect(Collectors.toSet());
    }

    @Override
    public long countSessionsFor(final String principalId) {
        if (hazelcastInstance.getJet().getConfig().isEnabled()) {
            val md = ticketCatalog.find(TicketGrantingTicket.PREFIX);
            val sql = String.format("SELECT COUNT(*) FROM %s WHERE principal=?", md.getProperties().getStorageName());
            LOGGER.debug("Executing SQL query [{}]", sql);
            val results = hazelcastInstance.getSql().execute(sql, encodeTicketId(principalId));
            return results.iterator().next().getObject(0);
        }
        return super.countSessionsFor(principalId);
    }

    @Override
    public Stream<? extends Ticket> getSessionsFor(final String principalId) {
        if (hazelcastInstance.getJet().getConfig().isEnabled()) {
            val md = ticketCatalog.find(TicketGrantingTicket.PREFIX);
            val sql = String.format("SELECT * FROM %s WHERE principal=?", md.getProperties().getStorageName());
            LOGGER.debug("Executing SQL query [{}]", sql);
            val results = hazelcastInstance.getSql().execute(sql, encodeTicketId(principalId));
            return StreamSupport.stream(results.spliterator(), false)
                .map(row -> {
                    val ticket = (Ticket) row.getObject("ticket");
                    return decodeTicket(ticket);
                });
        }
        return super.getSessionsFor(principalId);
    }

    /**
     * Make sure we shutdown HazelCast when the context is destroyed.
     */
    public void shutdown() {
        try {
            LOGGER.info("Shutting down Hazelcast instance [{}]", this.hazelcastInstance.getConfig().getInstanceName());
            this.hazelcastInstance.shutdown();
        } catch (final Exception e) {
            LOGGER.debug(e.getMessage());
        }
    }

    @Override
    public void destroy() {
        close();
    }

    @Override
    public void close() {
        shutdown();
    }

    private IMap<String, HazelcastTicketHolder> getTicketMapInstanceByMetadata(final TicketDefinition metadata) {
        val mapName = metadata.getProperties().getStorageName();
        LOGGER.debug("Locating map name [{}] for ticket definition [{}]", mapName, metadata);
        return getTicketMapInstance(mapName);
    }

    private IMap<String, HazelcastTicketHolder> getTicketMapInstance(
        @NonNull
        final String mapName) {
        try {
            val inst = hazelcastInstance.<String, HazelcastTicketHolder>getMap(mapName);
            LOGGER.debug("Located Hazelcast map instance [{}]", mapName);
            return inst;
        } catch (final Exception e) {
            LoggingUtils.error(LOGGER, e);
        }
        return null;
    }
}
