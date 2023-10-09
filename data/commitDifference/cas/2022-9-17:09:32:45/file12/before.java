package org.apereo.cas.logout;

import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.logout.slo.SingleLogoutRequestContext;
import org.apereo.cas.logout.slo.SingleLogoutServiceMessageHandler;
import org.apereo.cas.ticket.AuthenticatedServicesAwareTicketGrantingTicket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This logout manager handles the Single Log Out process.
 *
 * @author Jerome Leleu
 * @since 4.0.0
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public class DefaultLogoutManager implements LogoutManager {
    private final boolean singleLogoutCallbacksDisabled;

    private final LogoutExecutionPlan logoutExecutionPlan;

    @Override
    public List<SingleLogoutRequestContext> performLogout(final SingleLogoutExecutionRequest context) {
        val ticket = context.getTicketGrantingTicket();
        LOGGER.info("Performing logout operations for [{}]", ticket.getId());
        if (this.singleLogoutCallbacksDisabled) {
            LOGGER.info("Single logout callbacks are disabled");
            return new ArrayList<>(0);
        }
        val logoutRequests = performLogoutForTicket(context);
        logoutExecutionPlan.getLogoutPostProcessors().forEach(h -> {
            LOGGER.debug("Invoking logout handler [{}] to process ticket [{}]", h.getClass().getSimpleName(), ticket.getId());
            h.handle(ticket);
        });
        LOGGER.info("[{}] logout requests were processed", logoutRequests.size());
        return logoutRequests;
    }

    private List<SingleLogoutRequestContext> performLogoutForTicket(final SingleLogoutExecutionRequest context) {
        val ticketToBeLoggedOut = context.getTicketGrantingTicket();
        val streamServices = new LinkedHashMap<String, Service>();
        if (ticketToBeLoggedOut instanceof AuthenticatedServicesAwareTicketGrantingTicket) {
            val services = ((AuthenticatedServicesAwareTicketGrantingTicket) ticketToBeLoggedOut).getServices();
            streamServices.putAll(services);
        }
        streamServices.putAll(ticketToBeLoggedOut.getProxyGrantingTickets());
        val logoutServices = streamServices
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() instanceof WebApplicationService)
            .filter(Objects::nonNull)
            .map(entry -> Pair.of(entry.getKey(), (WebApplicationService) entry.getValue()))
            .collect(Collectors.toList());

        val sloHandlers = logoutExecutionPlan.getSingleLogoutServiceMessageHandlers();
        return logoutServices
            .stream()
            .map(entry -> sloHandlers
                .stream()
                .sorted(Comparator.comparing(SingleLogoutServiceMessageHandler::getOrder))
                .filter(handler -> handler.supports(context, entry.getValue()))
                .map(handler -> {
                    val service = entry.getValue();
                    LOGGER.trace("Handling single logout callback for [{}]", service.getId());
                    return handler.handle(service, entry.getKey(), context);
                })
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .filter(distinctByKey(SingleLogoutRequestContext::getService))
            .collect(Collectors.toList());
    }

    private static <T> Predicate<T> distinctByKey(final Function<? super T, Object> keyExtractor) {
        val seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
