package org.apereo.cas.web.flow.login;

import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.model.TriStateBoolean;
import org.apereo.cas.web.cookie.CasCookieBuilder;
import org.apereo.cas.web.flow.SingleSignOnParticipationRequest;
import org.apereo.cas.web.flow.SingleSignOnParticipationStrategy;
import org.apereo.cas.web.flow.actions.BaseCasWebflowAction;
import org.apereo.cas.web.support.WebUtils;
import org.apereo.cas.web.support.gen.CookieRetrievingCookieGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Action that handles the TicketGrantingTicket creation and destruction. If the
 * action is given a TicketGrantingTicket and one also already exists, the old
 * one is destroyed and replaced with the new one. This action always returns
 * "success".
 *
 * @author Scott Battaglia
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class SendTicketGrantingTicketAction extends BaseCasWebflowAction {
    private final TicketRegistry ticketRegistry;

    private final CasCookieBuilder ticketGrantingTicketCookieGenerator;

    private final SingleSignOnParticipationStrategy singleSignOnParticipationStrategy;

    @Override
    protected Event doExecute(final RequestContext context) throws Exception {
        val ticketGrantingTicketId = WebUtils.getTicketGrantingTicketId(context);
        val ticketGrantingTicketValueFromCookie = WebUtils.getTicketGrantingTicketIdFrom(context.getFlowScope());

        if (StringUtils.isBlank(ticketGrantingTicketId)) {
            LOGGER.debug("No ticket-granting ticket is found in the context.");
            return success();
        }

        val ssoRequest = SingleSignOnParticipationRequest.builder()
            .requestContext(context)
            .build();
        if (WebUtils.isAuthenticatingAtPublicWorkstation(context)) {
            LOGGER.info("Authentication is at a public workstation. SSO cookie will not be generated");
        } else if (this.singleSignOnParticipationStrategy.supports(ssoRequest)) {
            val createCookie = singleSignOnParticipationStrategy.isCreateCookieOnRenewedAuthentication(ssoRequest) == TriStateBoolean.TRUE
                               || singleSignOnParticipationStrategy.isParticipating(ssoRequest);
            if (createCookie) {
                LOGGER.debug("Setting ticket-granting cookie for current session linked to [{}].", ticketGrantingTicketId);
                val request = WebUtils.getHttpServletRequestFromExternalWebflowContext(context);
                val response = WebUtils.getHttpServletResponseFromExternalWebflowContext(context);
                ticketGrantingTicketCookieGenerator.addCookie(request, response,
                    CookieRetrievingCookieGenerator.isRememberMeAuthentication(context), ticketGrantingTicketId);
            } else {
                LOGGER.info("Authentication session is renewed but CAS is not configured to create the SSO session. "
                            + "SSO cookie will not be generated. Subsequent requests will be challenged for credentials.");
            }
        }

        if (ticketGrantingTicketValueFromCookie != null && !ticketGrantingTicketId.equals(ticketGrantingTicketValueFromCookie)) {
            LOGGER.debug("Ticket-granting ticket from ticket-granting cookie does not match the ticket-granting ticket from context");
            ticketRegistry.deleteTicket(ticketGrantingTicketValueFromCookie);
        }

        return success();
    }
}
