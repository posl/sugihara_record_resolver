package org.apereo.cas.ticket;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * This is {@link org.apereo.cas.ticket.RenewableServiceTicket}.
 *
 * @author Misagh Moayyed
 * @since 6.5.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface RenewableServiceTicket extends Ticket {
    /**
     * Determine if this ticket was created at the same time as a
     * {@link TicketGrantingTicket}.
     *
     * @return true if it is, false otherwise.
     */
    boolean isFromNewLogin();
}
