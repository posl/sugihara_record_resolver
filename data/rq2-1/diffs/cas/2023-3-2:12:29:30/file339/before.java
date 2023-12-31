package org.apereo.cas.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * This is {@link AMQPTicketRegistryTicketCatalogConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 7.0.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.TicketRegistry, module = "amqp")
@AutoConfiguration
public class AMQPTicketRegistryTicketCatalogConfiguration extends BaseTicketDefinitionBuilderSupportConfiguration {

    public AMQPTicketRegistryTicketCatalogConfiguration(
        final ConfigurableApplicationContext applicationContext,
        final CasConfigurationProperties casProperties) {
        super(casProperties, new CasTicketCatalogConfigurationValuesProvider() {}, applicationContext);
    }
}
