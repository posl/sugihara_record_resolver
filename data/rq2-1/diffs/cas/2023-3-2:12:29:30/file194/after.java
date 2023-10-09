package org.apereo.cas.configuration.config.standalone;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.CasConfigurationPropertiesEnvironmentManager;
import org.apereo.cas.configuration.DefaultCasConfigurationPropertiesSourceLocator;
import org.apereo.cas.configuration.api.CasConfigurationPropertiesSourceLocator;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.configuration.loader.ConfigurationPropertiesLoaderFactory;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.io.ResourceLoader;

import java.util.List;
import java.util.Optional;

/**
 * This is {@link CasCoreBootstrapStandaloneConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@SuppressWarnings("ConditionalOnProperty")
@ConditionalOnProperty(value = "spring.cloud.config.enabled", havingValue = "false", matchIfMissing = true)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.CasConfiguration)
@AutoConfiguration
public class CasCoreBootstrapStandaloneConfiguration {

    @Configuration(value = "CasCoreBootstrapStandaloneSourcesConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    @Lazy(false)
    public static class CasCoreBootstrapStandaloneSourcesConfiguration implements PriorityOrdered {

        @Bean
        public PropertySourceLocator casCoreBootstrapPropertySourceLocator(
            final List<CasConfigurationPropertiesSourceLocator> locatorList,
            final ResourceLoader resourceLoader) {
            AnnotationAwareOrderComparator.sortIfNecessary(locatorList);
            return environment -> {
                val composite = new CompositePropertySource("casCoreBootstrapPropertySourceLocator");
                locatorList
                    .stream()
                    .map(locator -> locator.locate(environment, resourceLoader))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(composite::addPropertySource);
                return composite;
            };
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }
    }

    @Configuration(value = "CasCoreBootstrapStandaloneLocatorConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    @Profile({
        CasConfigurationPropertiesSourceLocator.PROFILE_STANDALONE,
        CasConfigurationPropertiesSourceLocator.PROFILE_EMBEDDED
    })
    @Lazy(false)
    public static class CasCoreBootstrapStandaloneLocatorConfiguration {
        @ConditionalOnMissingBean(name = "casConfigurationPropertiesSourceLocator")
        @Bean
        public CasConfigurationPropertiesSourceLocator casConfigurationPropertiesSourceLocator(
            @Qualifier("configurationPropertiesLoaderFactory")
            final ConfigurationPropertiesLoaderFactory configurationPropertiesLoaderFactory,
            @Qualifier(CasConfigurationPropertiesEnvironmentManager.BEAN_NAME)
            final CasConfigurationPropertiesEnvironmentManager configurationPropertiesEnvironmentManager) {
            return new DefaultCasConfigurationPropertiesSourceLocator(
                configurationPropertiesEnvironmentManager,
                configurationPropertiesLoaderFactory);
        }
    }
}
