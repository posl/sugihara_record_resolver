package org.apereo.cas.config;

import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.CoreAuthenticationUtils;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.discovery.CasServerDiscoveryProfileEndpoint;
import org.apereo.cas.discovery.CasServerProfileRegistrar;
import org.apereo.cas.util.spring.beans.BeanContainer;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import lombok.val;
import org.apereo.services.persondir.IPersonAttributeDao;
import org.apereo.services.persondir.IPersonAttributeDaoFilter;
import org.pac4j.core.client.Clients;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This is {@link CasDiscoveryProfileConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.Discovery)
@AutoConfiguration
public class CasDiscoveryProfileConfiguration {

    private static Set<String> transformAttributes(final List<String> attributes) {
        val attributeSet = new LinkedHashSet<String>(attributes.size());
        CoreAuthenticationUtils.transformPrincipalAttributesListIntoMultiMap(attributes).values().forEach(v -> attributeSet.add(v.toString()));
        return attributeSet;
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public CasServerProfileRegistrar casServerProfileRegistrar(
        final CasConfigurationProperties casProperties,
        @Qualifier("builtClients")
        final ObjectProvider<Clients> builtClients,
        @Qualifier("discoveryProfileAvailableAttributes")
        final BeanContainer<String> discoveryProfileAvailableAttributes,
        @Qualifier("authenticationEventExecutionPlan")
        final AuthenticationEventExecutionPlan authenticationEventExecutionPlan) {
        return new CasServerProfileRegistrar(casProperties, builtClients.getIfAvailable(),
            discoveryProfileAvailableAttributes.toSet(), authenticationEventExecutionPlan);
    }

    @Bean
    @ConditionalOnAvailableEndpoint
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public CasServerDiscoveryProfileEndpoint discoveryProfileEndpoint(
        final CasConfigurationProperties casProperties,
        @Qualifier("casServerProfileRegistrar")
        final CasServerProfileRegistrar casServerProfileRegistrar) {
        return new CasServerDiscoveryProfileEndpoint(casProperties, casServerProfileRegistrar);
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public BeanContainer<String> discoveryProfileAvailableAttributes(
        final CasConfigurationProperties casProperties,
        @Qualifier(PrincipalResolver.BEAN_NAME_ATTRIBUTE_REPOSITORY)
        final IPersonAttributeDao attributeRepository) {
        val attributes = new LinkedHashSet<String>(0);
        val possibleUserAttributeNames = attributeRepository.getPossibleUserAttributeNames(IPersonAttributeDaoFilter.alwaysChoose());
        if (possibleUserAttributeNames != null) {
            attributes.addAll(possibleUserAttributeNames);
        }
        val ldapProps = casProperties.getAuthn().getLdap();
        if (ldapProps != null) {
            ldapProps.forEach(ldap -> {
                attributes.addAll(transformAttributes(ldap.getPrincipalAttributeList()));
                attributes.addAll(transformAttributes(ldap.getAdditionalAttributes()));
            });
        }
        val jdbcProps = casProperties.getAuthn().getJdbc();
        if (jdbcProps != null) {
            jdbcProps.getQuery().forEach(jdbc -> attributes.addAll(transformAttributes(jdbc.getPrincipalAttributeList())));
        }
        return BeanContainer.of(attributes);
    }
}
