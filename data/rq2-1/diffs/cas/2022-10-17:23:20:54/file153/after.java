package org.apereo.cas.pac4j;

import org.apereo.cas.ticket.TicketFactory;
import org.apereo.cas.ticket.TransientSessionTicket;
import org.apereo.cas.ticket.TransientSessionTicketFactory;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.function.FunctionUtils;
import org.apereo.cas.web.cookie.CasCookieBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.jee.context.JEEContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

/**
 * This is {@link DistributedJEESessionStore}.
 *
 * @author Misagh Moayyed
 * @author Jerome LELEU
 * @since 6.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class DistributedJEESessionStore implements SessionStore {
    /**
     * Default session store implementation bean.
     */
    public static final String DEFAULT_BEAN_NAME = "samlIdPDistributedSessionStore";

    private static final String SESSION_ID_IN_REQUEST_ATTRIBUTE = "sessionIdInRequestAttribute";

    private final TicketRegistry ticketRegistry;

    private final TicketFactory ticketFactory;

    private final CasCookieBuilder cookieGenerator;
    
    @Override
    public Optional<String> getSessionId(final WebContext webContext, final boolean create) {
        var sessionId = fetchSessionIdFromContext(webContext);
        if (StringUtils.isBlank(sessionId) && create) {
            sessionId = UUID.randomUUID().toString();
            LOGGER.trace("Generated session id: [{}]", sessionId);
            
            val context = JEEContext.class.cast(webContext);
            cookieGenerator.addCookie(context.getNativeRequest(), context.getNativeResponse(), sessionId);
            context.setRequestAttribute(SESSION_ID_IN_REQUEST_ATTRIBUTE, sessionId);
        }
        return Optional.ofNullable(sessionId);
    }

    @Override
    public Optional get(final WebContext context, final String key) {
        LOGGER.trace("Getting key: [{}]", key);
        val ticket = getTransientSessionTicketForSession(context);
        if (ticket == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ticket.getProperties().get(key));
    }

    @Override
    public void set(final WebContext context, final String key, final Object value) {
        LOGGER.trace("Setting key: [{}]", key);
        val sessionId = getSessionId(context, true).get();

        val properties = new HashMap<String, Serializable>();
        if (value instanceof Serializable) {
            properties.put(key, (Serializable) value);
        } else if (value != null) {
            LOGGER.warn("Object value [{}] assigned to [{}] is not serializable and may not be part of the ticket [{}]", value, key, sessionId);
        }

        val ticket = getTransientSessionTicketForSession(context);
        if (value == null && ticket != null) {
            ticket.getProperties().remove(key);
            FunctionUtils.doUnchecked(__ -> ticketRegistry.updateTicket(ticket));
        } else if (ticket == null) {
            val transientFactory = (TransientSessionTicketFactory) this.ticketFactory.get(TransientSessionTicket.class);
            val created = transientFactory.create(sessionId, properties);
            FunctionUtils.doUnchecked(__ -> ticketRegistry.addTicket(created));
        } else {
            ticket.getProperties().putAll(properties);
            FunctionUtils.doUnchecked(__ -> ticketRegistry.updateTicket(ticket));
        }
    }

    @Override
    public boolean destroySession(final WebContext webContext) {
        val sessionId = fetchSessionIdFromContext(webContext);
        if (sessionId != null) {
            val ticketId = TransientSessionTicketFactory.normalizeTicketId(sessionId);
            FunctionUtils.doUnchecked(__ -> ticketRegistry.deleteTicket(ticketId));

            val context = JEEContext.class.cast(webContext);
            cookieGenerator.removeCookie(context.getNativeResponse());
            LOGGER.trace("Removes session cookie and ticket: [{}]", ticketId);
        }
        return true;
    }

    @Override
    public Optional getTrackableSession(final WebContext context) {
        val sessionId = fetchSessionIdFromContext(context);
        LOGGER.trace("Track sessionId: [{}]", sessionId);
        return Optional.ofNullable(sessionId);
    }

    @Override
    public Optional<SessionStore> buildFromTrackableSession(final WebContext context, final Object trackableSession) {
        context.setRequestAttribute(SESSION_ID_IN_REQUEST_ATTRIBUTE, trackableSession);
        LOGGER.trace("Force sessionId: [{}]", trackableSession);
        return Optional.of(this);
    }

    @Override
    public boolean renewSession(final WebContext context) {
        return false;
    }

    /**
     * Fetch session id from context.
     *
     * @param webContext the web context
     * @return the string
     */
    protected String fetchSessionIdFromContext(final WebContext webContext) {
        var sessionId = (String) webContext.getRequestAttribute(SESSION_ID_IN_REQUEST_ATTRIBUTE).orElse(null);
        if (StringUtils.isBlank(sessionId)) {
            val context = JEEContext.class.cast(webContext);
            sessionId = cookieGenerator.retrieveCookieValue(context.getNativeRequest());
        }
        LOGGER.trace("Fetched session id: [{}]", sessionId);
        return sessionId;
    }

    private TransientSessionTicket getTransientSessionTicketForSession(final WebContext context) {
        try {
            val sessionId = fetchSessionIdFromContext(context);
            if (sessionId != null) {
                val ticketId = TransientSessionTicketFactory.normalizeTicketId(sessionId);

                LOGGER.trace("fetching ticket: [{}]", ticketId);
                return ticketRegistry.getTicket(ticketId, TransientSessionTicket.class);
            }
        } catch (final Exception e) {
            LOGGER.trace(e.getMessage(), e);
        }
        return null;
    }
}
