package org.apereo.cas.oidc.web;

import org.apereo.cas.oidc.OidcConstants;
import org.apereo.cas.oidc.util.OidcRequestSupport;
import org.apereo.cas.support.oauth.web.OAuth20RequestParameterResolver;
import org.apereo.cas.support.oauth.web.OAuth20TicketGrantingTicketAwareSecurityLogic;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.web.cookie.CasCookieBuilder;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.pac4j.core.client.Client;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.BasicUserProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * This is {@link OidcAuthenticationAuthorizeSecurityLogic}.
 *
 * @author Misagh Moayyed
 * @since 6.4.0
 */
@Slf4j
public class OidcAuthenticationAuthorizeSecurityLogic extends OAuth20TicketGrantingTicketAwareSecurityLogic {
    private final OAuth20RequestParameterResolver oauthRequestParameterResolver;

    public OidcAuthenticationAuthorizeSecurityLogic(final CasCookieBuilder ticketGrantingTicketCookieGenerator,
                                                    final TicketRegistry ticketRegistry,
                                                    final OAuth20RequestParameterResolver parameterResolver) {
        super(ticketGrantingTicketCookieGenerator, ticketRegistry);
        this.oauthRequestParameterResolver = parameterResolver;
    }


    @Override
    protected List<UserProfile> loadProfiles(final ProfileManager manager, final WebContext context,
                                             final SessionStore sessionStore, final List<Client> clients) {
        val prompts = oauthRequestParameterResolver.resolveSupportedPromptValues(context);
        LOGGER.debug("Located OpenID Connect prompts from request as [{}]", prompts);

        val tooOld = OidcRequestSupport.getOidcMaxAgeFromAuthorizationRequest(context)
            .map(maxAge -> manager.getProfile(BasicUserProfile.class)
                .stream()
                .anyMatch(profile -> OidcRequestSupport.isCasAuthenticationOldForMaxAgeAuthorizationRequest(context, profile)))
            .orElse(Boolean.FALSE);

        return tooOld || prompts.contains(OidcConstants.PROMPT_LOGIN)
            ? new ArrayList<>(0)
            : super.loadProfiles(manager, context, sessionStore, clients);
    }
}
