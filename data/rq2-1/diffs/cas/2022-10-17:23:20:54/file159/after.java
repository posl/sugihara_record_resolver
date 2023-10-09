package org.apereo.cas.web.flow.actions.logout;

import org.apereo.cas.support.saml.SamlProtocolConstants;
import org.apereo.cas.util.LoggingUtils;
import org.apereo.cas.web.flow.actions.BaseCasWebflowAction;
import org.apereo.cas.web.support.WebUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.jee.http.adapter.JEEHttpActionAdapter;
import org.pac4j.saml.client.SAML2Client;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import java.util.Optional;

/**
 * This is {@link DelegatedAuthenticationClientFinishLogoutAction}.
 * <p>
 * The action takes into account the currently used PAC4J client which is stored
 * in the user profile. If the client is found, its logout action is executed.
 * <p>
 * Assumption: The PAC4J user profile should be in the user session during
 * logout, accessible with PAC4J Profile Manager. The Logout web flow should
 * make sure the user profile is present.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class DelegatedAuthenticationClientFinishLogoutAction extends BaseCasWebflowAction {
    private final Clients clients;

    private final SessionStore sessionStore;

    @Override
    protected Event doExecute(final RequestContext requestContext) {
        val request = WebUtils.getHttpServletRequestFromExternalWebflowContext(requestContext);
        val response = WebUtils.getHttpServletResponseFromExternalWebflowContext(requestContext);
        val context = new JEEContext(request, response);

        var clientName = WebUtils.getDelegatedAuthenticationClientName(requestContext);
        if (clientName == null) {
            clientName = requestContext.getRequestParameters().get(SamlProtocolConstants.PARAMETER_SAML_RELAY_STATE);
            if (StringUtils.isNotBlank(clientName)) {
                clients.findClient(clientName)
                    .filter(client -> client instanceof SAML2Client)
                    .map(SAML2Client.class::cast)
                    .ifPresent(client -> {
                        try {
                            LOGGER.debug("Located client from relay-state: [{}]", client);
                            val samlContext = client.getContextProvider().buildContext(client, context, this.sessionStore);
                            client.getLogoutProfileHandler().receive(samlContext);
                        } catch (final HttpAction action) {
                            LOGGER.debug("Adapting logout response via [{}]", action.toString());
                            JEEHttpActionAdapter.INSTANCE.adapt(action, context);
                        } catch (final Exception e) {
                            LoggingUtils.error(LOGGER, e);
                        }
                    });
            }
        } else {
            val logoutRedirect = WebUtils.getLogoutRedirectUrl(requestContext, String.class);
            clients.findClient(clientName)
                .filter(client -> client instanceof SAML2Client)
                .map(SAML2Client.class::cast)
                .ifPresent(client -> {
                    val logoutRequest = WebUtils.getDelegatedAuthenticationLogoutRequest(requestContext,
                        DelegatedAuthenticationClientLogoutRequest.class);
                    Optional.ofNullable(logoutRequest)
                        .filter(r -> StringUtils.isNotBlank(logoutRedirect))
                        .ifPresent(__ -> {
                            LOGGER.debug("Located client from webflow state: [{}]", client);
                            val validator = client.getLogoutValidator();
                            validator.setPostLogoutURL(logoutRedirect);
                            LOGGER.debug("Captured post logout url: [{}]", logoutRedirect);
                            WebUtils.putLogoutRedirectUrl(requestContext, null);
                        });
                });
        }
        return null;
    }

}
