package org.apereo.cas.authentication;

import org.apereo.cas.authentication.principal.Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * This is {@link AuthenticationResultBuilder}. It attempts to collect authentication objects
 * and will put the computed finalized primary {@link Authentication} into {@link AuthenticationResult}.
 * <strong>Concurrency semantics: implementations MUST be thread-safe.</strong>
 * Instances of this class should never be declared as a field. Rather they should always be passed around to methods that need them.
 *
 * @author Misagh Moayyed
 * @since 4.2.0
 */
public interface AuthenticationResultBuilder extends Serializable {

    /**
     * Gets authentications.
     *
     * @return the authentications
     */
    Set<Authentication> getAuthentications();

    /**
     * Gets the initial authentication.
     *
     * @return the initial authentication
     */
    Optional<Authentication> getInitialAuthentication();

    /**
     * Gets initial credential.
     *
     * @return the initial credential
     */
    Optional<Credential> getInitialCredential();

    /**
     * Collect authentication objects from any number of processed authentication transactions.
     *
     * @param authentication the authentication
     * @return the authentication result builder
     */
    AuthenticationResultBuilder collect(Authentication authentication);

    /**
     * Collect authentication result builder.
     *
     * @param authentications the authentication
     * @return the authentication result builder
     */
    AuthenticationResultBuilder collect(Collection<Authentication> authentications);

    /**
     * Provided credentials immediately by the user.
     *
     * @param credential the credential
     * @return the authentication context builder
     */
    AuthenticationResultBuilder collect(Credential credential);

    /**
     * Provided credential metadata collected for the authentication transaction.
     * Metadata represents arbitrary details linked to a credential
     * such as browser user agent, etc that do not strictly belong to a credential type.
     *
     * @param credential the credential
     * @return the authentication context builder
     */
    AuthenticationResultBuilder collect(CredentialMetaData credential);

    /**
     * Build authentication result.
     *
     * @param principalElectionStrategy a principalElectionStrategy to use
     * @return the authentication result
     */
    AuthenticationResult build(PrincipalElectionStrategy principalElectionStrategy);

    /**
     * Build authentication result.
     *
     * @param principalElectionStrategy a principalElectionStrategy to use
     * @param service                   the service
     * @return the authentication result
     */
    AuthenticationResult build(PrincipalElectionStrategy principalElectionStrategy, Service service);
}
