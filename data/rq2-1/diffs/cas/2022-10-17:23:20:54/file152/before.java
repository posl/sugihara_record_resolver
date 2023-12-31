package org.apereo.cas.authentication.principal;

import org.springframework.webflow.execution.RequestContext;

import java.util.List;

/**
 * This is {@link DelegatedClientAuthenticationCredentialResolver}.
 *
 * @author Misagh Moayyed
 * @since 6.6.0
 */
public interface DelegatedClientAuthenticationCredentialResolver {
    /**
     * Supports this client credential?
     *
     * @param credentials the credentials
     * @return the boolean
     */
    boolean supports(ClientCredential credentials);

    /**
     * Resolve list of candidate profiles.
     *
     * @param context     the context
     * @param credentials the credentials
     * @return the list
     */
    List<DelegatedAuthenticationCandidateProfile> resolve(RequestContext context, ClientCredential credentials);
}
