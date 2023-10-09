package org.apereo.cas.discovery;

import org.apereo.cas.CentralAuthenticationService;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.authentication.MultifactorAuthenticationUtils;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.BaseRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.ticket.TicketCatalog;
import org.apereo.cas.ticket.TicketDefinition;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.ReflectionUtils;
import org.apereo.cas.util.function.FunctionUtils;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.val;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is {@link CasServerProfileRegistrar}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Setter
@RequiredArgsConstructor
public class CasServerProfileRegistrar implements ApplicationContextAware {
    private final CasConfigurationProperties casProperties;

    private final Clients clients;

    private final Set<String> availableAttributes;

    private final AuthenticationEventExecutionPlan authenticationEventExecutionPlan;

    private ApplicationContext applicationContext;

    private static Set<String> locateRegisteredServiceTypesSupported() {

        val subTypes = ReflectionUtils.findSubclassesInPackage(BaseRegisteredService.class, CentralAuthenticationService.NAMESPACE);
        return subTypes.stream()
            .filter(type -> !type.isInterface() && !Modifier.isAbstract(type.getModifiers()))
            .map(type -> FunctionUtils.doAndHandle(() -> {
                val service = (RegisteredService) type.getDeclaredConstructor().newInstance();
                return service.getFriendlyName() + '@' + service.getClass().getName();
            }))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }


    /**
     * Gets profile.
     *
     * @return the profile
     */
    public CasServerProfile getProfile() {
        val profile = new CasServerProfile();
        profile.setRegisteredServiceTypesSupported(locateRegisteredServiceTypesSupported());
        profile.setMultifactorAuthenticationProviderTypesSupported(locateMultifactorAuthenticationProviderTypesSupported());
        profile.setDelegatedClientTypesSupported(locateDelegatedClientTypesSupported());
        profile.setAvailableAttributes(this.availableAttributes);
        profile.setUserDefinedScopes(casProperties.getAuthn().getOidc().getCore().getUserDefinedScopes().keySet());
        profile.setAvailableAuthenticationHandlers(locateAvailableAuthenticationHandlers());
        profile.setTicketTypesSupported(locateTicketTypesSupported());
        return profile;
    }

    private Map<String, Map<String, Object>> locateTicketTypesSupported() {
        val catalog = applicationContext.getBean(TicketCatalog.BEAN_NAME, TicketCatalog.class);
        return catalog
            .findAll()
            .stream()
            .collect(Collectors.toMap(TicketDefinition::getPrefix,
                value -> CollectionUtils.wrap("storageName", value.getProperties().getStorageName(),
                    "storageTimeout", value.getProperties().getStorageTimeout())));
    }

    private Map<String, String> locateMultifactorAuthenticationProviderTypesSupported() {
        val providers = MultifactorAuthenticationUtils.getAvailableMultifactorAuthenticationProviders(applicationContext);
        return providers
            .values()
            .stream()
            .collect(Collectors.toMap(MultifactorAuthenticationProvider::getId, MultifactorAuthenticationProvider::getFriendlyName));
    }

    private Set<String> locateDelegatedClientTypesSupported() {
        return clients == null
            ? new LinkedHashSet<>(0)
            : clients.findAllClients().stream().map(Client::getName).collect(Collectors.toSet());
    }

    private Set<String> locateAvailableAuthenticationHandlers() {
        return this.authenticationEventExecutionPlan.getAuthenticationHandlers()
            .stream()
            .map(AuthenticationHandler::getName)
            .collect(Collectors.toSet());
    }
}
