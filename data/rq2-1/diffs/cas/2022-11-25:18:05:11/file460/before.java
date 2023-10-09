package org.apereo.cas.ticket.registry;

import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.util.crypto.CipherExecutor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

/**
 * This is {@link AbstractMapBasedTicketRegistry}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractMapBasedTicketRegistry extends AbstractTicketRegistry {

    protected AbstractMapBasedTicketRegistry(final CipherExecutor cipherExecutor) {
        setCipherExecutor(cipherExecutor);
    }

    @Override
    public void addTicketInternal(final Ticket ticket) throws Exception {
        val encTicket = encodeTicket(ticket);
        LOGGER.debug("Putting ticket [{}] in registry.", ticket.getId());
        getMapInstance().put(encTicket.getId(), encTicket);
    }

    @Override
    public Ticket getTicket(final String ticketId, final Predicate<Ticket> predicate) {
        val encTicketId = encodeTicketId(ticketId);
        if (StringUtils.isBlank(ticketId)) {
            return null;
        }
        val found = getMapInstance().get(encTicketId);
        if (found == null) {
            LOGGER.debug("Ticket [{}] could not be found", encTicketId);
            return null;
        }

        val result = decodeTicket(found);
        if (!predicate.test(result)) {
            LOGGER.debug("Cannot successfully fetch ticket [{}]", ticketId);
            return null;
        }
        return result;
    }

    @Override
    public long deleteSingleTicket(final String ticketId) {
        val encTicketId = encodeTicketId(ticketId);
        return !StringUtils.isBlank(encTicketId) && getMapInstance().remove(encTicketId) != null ? 1 : 0;
    }

    @Override
    public long deleteAll() {
        val size = getMapInstance().size();
        getMapInstance().clear();
        return size;
    }

    @Override
    public Collection<? extends Ticket> getTickets() {
        return decodeTickets(getMapInstance().values());
    }

    @Override
    public Ticket updateTicket(final Ticket ticket) throws Exception {
        LOGGER.trace("Updating ticket [{}] in registry...", ticket.getId());
        addTicket(ticket);
        return ticket;
    }

    /**
     * Create map instance, which must ben created during initialization phases
     * and always be the same instance.
     *
     * @return the map
     */
    public abstract Map<String, Ticket> getMapInstance();
}
