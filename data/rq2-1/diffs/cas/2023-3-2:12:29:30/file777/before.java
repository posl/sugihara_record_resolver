package org.apereo.cas.web.report;

import org.apereo.cas.authentication.CoreAuthenticationUtils;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.logout.slo.SingleLogoutRequestExecutor;
import org.apereo.cas.ticket.AuthenticatedServicesAwareTicketGrantingTicket;
import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.DateTimeUtils;
import org.apereo.cas.util.ISOStandardDateFormat;
import org.apereo.cas.util.LoggingUtils;
import org.apereo.cas.web.BaseCasActuatorEndpoint;

import io.swagger.v3.oas.annotations.Operation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SSO Report web controller that produces JSON data for the view.
 *
 * @author Misagh Moayyed
 * @author Dmitriy Kopylenko
 * @since 4.1
 */
@Slf4j
@ToString
@Getter
@RestControllerEndpoint(id = "ssoSessions", enableByDefault = false)
public class SingleSignOnSessionsEndpoint extends BaseCasActuatorEndpoint {

    private static final String STATUS = "status";

    private static final String TICKET_GRANTING_TICKET = "ticketGrantingTicket";

    private final ObjectProvider<TicketRegistry> ticketRegistryProvider;

    private final ObjectProvider<SingleLogoutRequestExecutor> singleLogoutRequestExecutor;

    public SingleSignOnSessionsEndpoint(
        final ObjectProvider<TicketRegistry> ticketRegistry,
        final CasConfigurationProperties casProperties,
        final ObjectProvider<SingleLogoutRequestExecutor> singleLogoutRequestExecutor) {
        super(casProperties);
        this.ticketRegistryProvider = ticketRegistry;
        this.singleLogoutRequestExecutor = singleLogoutRequestExecutor;
    }

    /**
     * Endpoint for getting SSO Sessions in JSON format.
     *
     * @param type     the type
     * @param username the username
     * @param from     the from
     * @param count    the count
     * @return the sso sessions
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all single sign-on sessions with the given type. The functionality provided here requires that the underlying "
                         + "ticket registry and store is able to store, maintain and return a collection tickets that represent the single sign-on session. "
                         + "You will not be able to collect and review sessions, if the ticket registry does not have this capability")
    public Map<String, Object> getSsoSessions(
        @Nullable
        @RequestParam(name = "type", required = false)
        final String type,
        @Nullable
        @RequestParam(name = "username", required = false)
        final String username,
        @RequestParam(name = "from", required = false, defaultValue = "0")
        final long from,
        @RequestParam(name = "count", required = false, defaultValue = "1000")
        final long count) {
        val sessionsMap = new HashMap<String, Object>();
        val option = Optional.ofNullable(type).map(SsoSessionReportOptions::valueOf).orElse(SsoSessionReportOptions.ALL);
        val activeSsoSessions = getActiveSsoSessions(option, username, from, count);
        sessionsMap.put("activeSsoSessions", activeSsoSessions);
        val totalTicketGrantingTickets = new AtomicLong();
        val totalProxyGrantingTickets = new AtomicLong();
        val totalUsageCount = new AtomicLong();
        val uniquePrincipals = new HashSet<>(activeSsoSessions.size());
        for (val activeSsoSession : activeSsoSessions) {
            if (activeSsoSession.containsKey(SsoSessionAttributeKeys.IS_PROXIED.getAttributeKey())) {
                val isProxied = Boolean.valueOf(activeSsoSession.get(SsoSessionAttributeKeys.IS_PROXIED.getAttributeKey()).toString());
                if (isProxied) {
                    totalProxyGrantingTickets.incrementAndGet();
                } else {
                    totalTicketGrantingTickets.incrementAndGet();
                    val principal = activeSsoSession.get(SsoSessionAttributeKeys.AUTHENTICATED_PRINCIPAL.getAttributeKey()).toString();
                    uniquePrincipals.add(principal);
                }
            } else {
                totalTicketGrantingTickets.incrementAndGet();
                val principal = activeSsoSession.get(SsoSessionAttributeKeys.AUTHENTICATED_PRINCIPAL.getAttributeKey()).toString();
                uniquePrincipals.add(principal);
            }
            val uses = Long.parseLong(activeSsoSession.get(SsoSessionAttributeKeys.NUMBER_OF_USES.getAttributeKey()).toString());
            totalUsageCount.getAndAdd(uses);
        }
        sessionsMap.put("totalProxyGrantingTickets", totalProxyGrantingTickets);
        sessionsMap.put("totalTicketGrantingTickets", totalTicketGrantingTickets);
        sessionsMap.put("totalTickets", totalTicketGrantingTickets.longValue() + totalProxyGrantingTickets.longValue());
        sessionsMap.put("totalPrincipals", uniquePrincipals.size());
        sessionsMap.put("totalUsageCount", totalUsageCount);
        return sessionsMap;
    }

    /**
     * Endpoint for destroying a single SSO Session.
     *
     * @param ticketGrantingTicket the ticket granting ticket
     * @param request              the request
     * @param response             the response
     * @return result map
     */
    @DeleteMapping(path = "/{ticketGrantingTicket}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Remove single sign-on session for ticket id")
    public Map<String, Object> destroySsoSession(
        @PathVariable
        final String ticketGrantingTicket,
        final HttpServletRequest request,
        final HttpServletResponse response) {
        val sessionsMap = new HashMap<String, Object>(1);
        try {
            val sloRequests = singleLogoutRequestExecutor.getObject().execute(ticketGrantingTicket, request, response);
            sessionsMap.put(STATUS, HttpServletResponse.SC_OK);
            sessionsMap.put(TICKET_GRANTING_TICKET, ticketGrantingTicket);
            sessionsMap.put("singleLogoutRequests", sloRequests);
        } catch (final Exception e) {
            LoggingUtils.error(LOGGER, e);
            sessionsMap.put(STATUS, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sessionsMap.put(TICKET_GRANTING_TICKET, ticketGrantingTicket);
            sessionsMap.put("message", e.getMessage());
        }
        return sessionsMap;
    }

    /**
     * Destroy sso sessions map.
     *
     * @param type     the type
     * @param username the username
     * @param from     the from
     * @param count    the count
     * @param request  the request
     * @param response the response
     * @return the map
     */
    @Operation(summary = "Remove single sign-on session for type and user")
    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> destroySsoSessions(
        @Nullable
        @RequestParam(name = "type", required = false)
        final String type,
        @Nullable
        @RequestParam(name = "username", required = false)
        final String username,
        @RequestParam(name = "from", required = false, defaultValue = "0")
        final long from,
        @RequestParam(name = "count", required = false, defaultValue = "1000")
        final long count,
        final HttpServletRequest request,
        final HttpServletResponse response) {
        if (StringUtils.isBlank(username) && StringUtils.isBlank(type)) {
            return Map.of(STATUS, HttpServletResponse.SC_BAD_REQUEST);
        }

        if (StringUtils.isNotBlank(username)) {
            val sessionsMap = new HashMap<String, Object>(1);
            var tickets = ticketRegistryProvider.getObject().getSessionsFor(username);
            if (from >= 0) {
                tickets = tickets.skip(from);
            }
            if (count >= 0) {
                tickets = tickets.limit(count);
            }
            tickets.forEach(ticket -> sessionsMap.put(ticket.getId(), destroySsoSession(ticket.getId(), request, response)));
            return sessionsMap;
        }

        val sessionsMap = new HashMap<String, Object>();
        val option = SsoSessionReportOptions.valueOf(type);
        val collection = getActiveSsoSessions(option, username, from, count);
        collection
            .stream()
            .map(sso -> sso.get(SsoSessionAttributeKeys.TICKET_GRANTING_TICKET.getAttributeKey()).toString())
            .forEach(ticketGrantingTicket -> destroySsoSession(ticketGrantingTicket, request, response));
        sessionsMap.put(STATUS, HttpServletResponse.SC_OK);
        return sessionsMap;
    }

    /**
     * The enum SSO session report options.
     */
    @RequiredArgsConstructor
    @Getter
    enum SsoSessionReportOptions {

        ALL("ALL"), PROXIED("PROXIED"), DIRECT("DIRECT");

        private final String type;
    }

    /**
     * The enum Sso session attribute keys.
     */
    @Getter
    enum SsoSessionAttributeKeys {
        AUTHENTICATED_PRINCIPAL("authenticated_principal"),
        PRINCIPAL_ATTRIBUTES("principal_attributes"),
        AUTHENTICATION_DATE("authentication_date"),
        AUTHENTICATION_DATE_FORMATTED("authentication_date_formatted"),
        TICKET_GRANTING_TICKET("ticket_granting_ticket"),
        AUTHENTICATION_ATTRIBUTES("authentication_attributes"),
        PROXIED_BY("proxied_by"),
        AUTHENTICATED_SERVICES("authenticated_services"),
        IS_PROXIED("is_proxied"),
        REMEMBER_ME("remember_me"),
        EXPIRATION_POLICY("expiration_policy"),
        NUMBER_OF_USES("number_of_uses");

        private final String attributeKey;

        /**
         * Instantiates a new Sso session attribute keys.
         *
         * @param attributeKey the attribute key
         */
        SsoSessionAttributeKeys(final String attributeKey) {
            this.attributeKey = attributeKey;
        }
    }

    private Collection<Map<String, Object>> getActiveSsoSessions(final SsoSessionReportOptions option,
                                                                 final String username,
                                                                 final long from,
                                                                 final long count) {
        val dateFormat = new ISOStandardDateFormat();
        return getNonExpiredTicketGrantingTickets(from, count)
            .map(TicketGrantingTicket.class::cast)
            .filter(tgt -> !(option == SsoSessionReportOptions.DIRECT && tgt.getProxiedBy() != null))
            .filter(tgt -> StringUtils.isBlank(username) || StringUtils.equalsIgnoreCase(username, tgt.getAuthentication().getPrincipal().getId()))
            .map(tgt -> {

                val authentication = tgt.getAuthentication();
                val principal = authentication.getPrincipal();
                val sso = new HashMap<String, Object>(SsoSessionAttributeKeys.values().length);
                sso.put(SsoSessionAttributeKeys.AUTHENTICATED_PRINCIPAL.getAttributeKey(), principal.getId());
                sso.put(SsoSessionAttributeKeys.AUTHENTICATION_DATE.getAttributeKey(), authentication.getAuthenticationDate());
                sso.put(SsoSessionAttributeKeys.AUTHENTICATION_DATE_FORMATTED.getAttributeKey(),
                    dateFormat.format(DateTimeUtils.dateOf(authentication.getAuthenticationDate())));
                sso.put(SsoSessionAttributeKeys.NUMBER_OF_USES.getAttributeKey(), tgt.getCountOfUses());
                sso.put(SsoSessionAttributeKeys.TICKET_GRANTING_TICKET.getAttributeKey(), tgt.getId());
                sso.put(SsoSessionAttributeKeys.PRINCIPAL_ATTRIBUTES.getAttributeKey(), principal.getAttributes());
                sso.put(SsoSessionAttributeKeys.AUTHENTICATION_ATTRIBUTES.getAttributeKey(), authentication.getAttributes());

                val policyData = new LinkedHashMap<String, Object>();

                val expirationPolicy = tgt.getExpirationPolicy();
                policyData.put("timeToIdle", expirationPolicy.getTimeToIdle());
                policyData.put("timeToLive", expirationPolicy.getTimeToLive());
                policyData.put("clock", expirationPolicy.getClock().toString());
                policyData.put("name", expirationPolicy.getName());

                Optional.ofNullable(expirationPolicy.getMaximumExpirationTime(tgt)).ifPresent(dt -> policyData.put("maxExpirationTime", dt));
                Optional.ofNullable(expirationPolicy.getIdleExpirationTime(tgt)).ifPresent(dt -> policyData.put("idleExpirationTime", dt));
                
                sso.put(SsoSessionAttributeKeys.EXPIRATION_POLICY.getAttributeKey(), policyData);
                sso.put(SsoSessionAttributeKeys.REMEMBER_ME.getAttributeKey(),
                    CoreAuthenticationUtils.isRememberMeAuthentication(authentication));
                if (option != SsoSessionReportOptions.DIRECT) {
                    if (tgt.getProxiedBy() != null) {
                        sso.put(SsoSessionAttributeKeys.IS_PROXIED.getAttributeKey(), Boolean.TRUE);
                        sso.put(SsoSessionAttributeKeys.PROXIED_BY.getAttributeKey(), tgt.getProxiedBy().getId());
                    } else {
                        sso.put(SsoSessionAttributeKeys.IS_PROXIED.getAttributeKey(), Boolean.FALSE);
                    }
                }

                if (tgt instanceof AuthenticatedServicesAwareTicketGrantingTicket) {
                    val services = ((AuthenticatedServicesAwareTicketGrantingTicket) tgt).getServices();
                    sso.put(SsoSessionAttributeKeys.AUTHENTICATED_SERVICES.getAttributeKey(), services);
                }
                return sso;
            })
            .collect(Collectors.toList());
    }

    private Stream<? extends Ticket> getNonExpiredTicketGrantingTickets(final long from, final long count) {
        var tickets = ticketRegistryProvider.getObject().getTickets(ticket -> ticket instanceof TicketGrantingTicket && !ticket.isExpired());
        if (from >= 0) {
            tickets = tickets.skip(from);
        }
        if (count >= 0) {
            tickets = tickets.limit(count);
        }
        return tickets;
    }

}
