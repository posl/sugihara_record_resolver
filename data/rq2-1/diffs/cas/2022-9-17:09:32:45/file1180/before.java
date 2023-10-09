package org.apereo.cas.ticket.registry;

import org.apereo.cas.ticket.ExpirationPolicy;
import org.apereo.cas.ticket.Ticket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.infinispan.Cache;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * This is {@link InfinispanTicketRegistry}. Infinispan is a distributed in-memory
 * key/value data store with optional schema.
 * It offers advanced functionality such as transactions, events, querying and distributed processing.
 * See <a href="http://infinispan.org/features/">http://infinispan.org/features/</a> for more info.
 *
 * @author Misagh Moayyed
 * @since 4.2.0
 * @deprecated since 6.6 and scheduled for removal.
 */
@Slf4j
@RequiredArgsConstructor
@Deprecated(since = "6.6")
public class InfinispanTicketRegistry extends AbstractTicketRegistry {
    private final Cache<String, Ticket> cache;

    @Override
    public Ticket updateTicket(final Ticket ticket) throws Exception {
        val encodedTicket = encodeTicket(ticket);
        this.cache.put(encodedTicket.getId(), encodedTicket);
        return ticket;
    }

    @Override
    public void addTicketInternal(final Ticket ticketToAdd) throws Exception {
        val ticket = encodeTicket(ticketToAdd);
        val expirationPolicy = ticketToAdd.getExpirationPolicy();
        val idleTime = expirationPolicy.getTimeToIdle() <= 0
            ? expirationPolicy.getTimeToLive()
            : expirationPolicy.getTimeToIdle();

        val ttl = getTimeToLive(expirationPolicy);
        LOGGER.debug("Adding ticket [{}] to cache to live [{}] seconds and stay idle for [{}] seconds", ticketToAdd.getId(), ttl, idleTime);
        this.cache.put(ticket.getId(), ticket, ttl, TimeUnit.SECONDS, idleTime, TimeUnit.SECONDS);
    }

    @Override
    public Ticket getTicket(final String ticketId, final Predicate<Ticket> predicate) {
        val encTicketId = encodeTicketId(ticketId);
        if (ticketId == null) {
            return null;
        }
        val result = decodeTicket(Ticket.class.cast(cache.get(encTicketId)));
        if (result != null && predicate.test(result)) {
            return result;
        }
        return null;
    }

    @Override
    public long deleteSingleTicket(final String ticketId) {
        this.cache.remove(encodeTicketId(ticketId));
        return 1;
    }

    @Override
    public long deleteAll() {
        val size = this.cache.size();
        this.cache.clear();
        return size;
    }
    
    @Override
    public Collection<? extends Ticket> getTickets() {
        return decodeTickets(this.cache.values());
    }

    private static Long getTimeToLive(final ExpirationPolicy expirationPolicy) {
        val timeToLive = expirationPolicy.getTimeToLive();
        return Long.MAX_VALUE == timeToLive ? Long.valueOf(Integer.MAX_VALUE) : timeToLive;
    }
}
