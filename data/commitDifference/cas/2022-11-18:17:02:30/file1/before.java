package org.apereo.cas.rest.authentication;

import org.apereo.cas.authentication.AuthenticationException;
import org.apereo.cas.authentication.AuthenticationResult;
import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.MultifactorAuthenticationTriggerSelectionStrategy;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.rest.BadRestRequestException;
import org.apereo.cas.rest.factory.RestHttpRequestCredentialFactory;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.validation.RequestedAuthenticationContextValidator;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.util.MultiValueMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;

/**
 * This is {@link DefaultRestAuthenticationService}.
 *
 * @author Misagh Moayyed
 * @since 6.4.0
 */
@Slf4j
public record DefaultRestAuthenticationService(AuthenticationSystemSupport authenticationSystemSupport, RestHttpRequestCredentialFactory credentialFactory,
                                               ServiceFactory<WebApplicationService> serviceFactory, MultifactorAuthenticationTriggerSelectionStrategy multifactorTriggerSelectionStrategy,
                                               ServicesManager servicesManager, RequestedAuthenticationContextValidator requestedContextValidator) implements RestAuthenticationService {
    @Override
    public Optional<AuthenticationResult> authenticate(final MultiValueMap<String, String> requestBody,
                                                       final HttpServletRequest request, final HttpServletResponse response) {
        val credentials = this.credentialFactory.fromRequest(request, requestBody);
        if (credentials == null || credentials.isEmpty()) {
            throw new BadRestRequestException("No credentials can be extracted to authenticate the REST request");
        }
        val service = this.serviceFactory.createService(request);
        val registeredService = servicesManager.findServiceBy(service);
        val authResult = Optional.ofNullable(
            authenticationSystemSupport.handleInitialAuthenticationTransaction(service, credentials.toArray(Credential[]::new)));

        return authResult
            .map(result -> result.getInitialAuthentication()
                .filter(authn -> !requestedContextValidator.validateAuthenticationContext(request, response, registeredService, authn, service).isSuccess())
                .map(authn ->
                    multifactorTriggerSelectionStrategy.resolve(request, response, registeredService, authn, service)
                        .map(provider -> {
                            LOGGER.debug("Extracting credentials for multifactor authentication via [{}]", provider);
                            val authnCredentials = credentialFactory.fromAuthentication(request, requestBody, authn, provider);
                            if (authnCredentials == null || authnCredentials.isEmpty()) {
                                throw new AuthenticationException("Unable to extract credentials for multifactor authentication");
                            }
                            return authenticationSystemSupport.finalizeAuthenticationTransaction(service, authnCredentials);
                        })
                        .orElseGet(() -> authenticationSystemSupport.finalizeAllAuthenticationTransactions(result, service)))
                .orElseGet(() -> authenticationSystemSupport.finalizeAllAuthenticationTransactions(result, service)));
    }
}
