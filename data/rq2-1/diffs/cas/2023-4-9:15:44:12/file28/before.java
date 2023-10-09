package org.apereo.cas.web.report;

import org.apereo.cas.CasViewConstants;
import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.DefaultAuthenticationBuilder;
import org.apereo.cas.authentication.credential.UsernamePasswordCredential;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicyContext;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.validation.DefaultAssertionBuilder;
import org.apereo.cas.validation.ImmutableAssertion;
import org.apereo.cas.web.BaseCasActuatorEndpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is {@link CasReleaseAttributesReportEndpoint}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Endpoint(id = "releaseAttributes", enableByDefault = false)
public class CasReleaseAttributesReportEndpoint extends BaseCasActuatorEndpoint {
    private final ObjectProvider<ServicesManager> servicesManager;

    private final ObjectProvider<AuthenticationSystemSupport> authenticationSystemSupport;

    private final ObjectProvider<ServiceFactory<WebApplicationService>> serviceFactory;

    private final ObjectProvider<PrincipalFactory> principalFactory;

    public CasReleaseAttributesReportEndpoint(final CasConfigurationProperties casProperties,
                                              final ObjectProvider<ServicesManager> servicesManager,
                                              final ObjectProvider<AuthenticationSystemSupport> authenticationSystemSupport,
                                              final ObjectProvider<ServiceFactory<WebApplicationService>> serviceFactory,
                                              final ObjectProvider<PrincipalFactory> principalFactory) {
        super(casProperties);
        this.servicesManager = servicesManager;
        this.authenticationSystemSupport = authenticationSystemSupport;
        this.serviceFactory = serviceFactory;
        this.principalFactory = principalFactory;
    }

    /**
     * Release principal attributes map.
     *
     * @param username the username
     * @param password the password
     * @param service  the service
     * @return the map
     */
    @ReadOperation
    @Operation(summary = "Get collection of released attributes for the user and application",
        parameters = {
            @Parameter(name = "username", required = true),
            @Parameter(name = "password", required = true),
            @Parameter(name = "service", required = true)
        })
    public Map<String, Object> releasePrincipalAttributes(final String username,
                                                          final String password,
                                                          final String service) {


        val selectedService = serviceFactory.getObject().createService(service);
        val registeredService = servicesManager.getObject().findServiceBy(selectedService);

        val credential = new UsernamePasswordCredential(username, password);
        val result = authenticationSystemSupport.getObject().finalizeAuthenticationTransaction(selectedService, credential);
        val authentication = result.getAuthentication();

        val principal = authentication.getPrincipal();
        val context = RegisteredServiceAttributeReleasePolicyContext.builder()
            .registeredService(registeredService)
            .service(selectedService)
            .principal(principal)
            .build();
        val attributesToRelease = registeredService.getAttributeReleasePolicy().getAttributes(context);
        val builder = DefaultAuthenticationBuilder.of(
            principal,
            principalFactory.getObject(),
            attributesToRelease,
            selectedService,
            registeredService,
            authentication);

        val finalAuthentication = builder.build();
        val assertion = DefaultAssertionBuilder.builder()
            .primaryAuthentication(finalAuthentication)
            .service(selectedService)
            .authentications(CollectionUtils.wrap(finalAuthentication))
            .registeredService(registeredService)
            .build()
            .assemble();

        val resValidation = new LinkedHashMap<String, Object>();
        resValidation.put(CasViewConstants.MODEL_ATTRIBUTE_NAME_ASSERTION, assertion);
        resValidation.put(CasViewConstants.MODEL_ATTRIBUTE_NAME_SERVICE, selectedService);
        resValidation.put("registeredService", registeredService);

        return resValidation;
    }

    /**
     * Method that accepts a JSON body through a POST method to receive user credentials and only returns a
     * map of attributes released for the authenticated user.
     *
     * @param username - the username
     * @param password - the password
     * @param service  - the service id
     * @return - the map
     */
    @WriteOperation
    @Operation(summary = "Get collection of released attributes for the user and application",
        parameters = {
            @Parameter(name = "username", required = true),
            @Parameter(name = "password", required = true),
            @Parameter(name = "service", required = true)
        })
    public Map<String, Object> releaseAttributes(final String username,
                                                 final String password,
                                                 final String service) {
        val map = releasePrincipalAttributes(username, password, service);
        val assertion = (ImmutableAssertion) map.get("assertion");
        return Map.of("uid", username, "attributes", assertion.primaryAuthentication().getPrincipal().getAttributes());
    }
}
