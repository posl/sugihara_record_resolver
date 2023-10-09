package org.apereo.cas.config;

import org.apereo.cas.authentication.adaptive.geo.GeoLocationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.support.geo.GeoLocationServiceConfigurer;
import org.apereo.cas.support.geo.GroovyGeoLocationService;
import org.apereo.cas.util.scripting.WatchableGroovyScriptResource;
import org.apereo.cas.util.spring.beans.BeanCondition;
import org.apereo.cas.util.spring.beans.BeanSupplier;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This is {@link CasGeoLocationConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 6.6.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.GeoLocation)
@AutoConfiguration
public class CasGeoLocationConfiguration {
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public GeoLocationService geoLocationService(
        final List<GeoLocationServiceConfigurer> providers) {
        val services = providers.stream()
            .map(GeoLocationServiceConfigurer::configure)
            .filter(BeanSupplier::isNotProxy)
            .sorted(AnnotationAwareOrderComparator.INSTANCE)
            .collect(Collectors.toList());
        return BeanSupplier.of(GeoLocationService.class)
            .when(!services.isEmpty())
            .supply(() -> services.get(0))
            .otherwiseProxy()
            .get();
    }

    @EnableConfigurationProperties(CasConfigurationProperties.class)
    @Configuration(value = "GroovyGeoLocationConfiguration", proxyBeanMethods = false)
    @ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.GeoLocation)
    public static class GroovyGeoLocationConfiguration {
        @ConditionalOnMissingBean(name = "groovyGeoLocationService")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public GeoLocationService groovyGeoLocationService(
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties) {
            return BeanSupplier.of(GeoLocationService.class)
                .when(BeanCondition.on("cas.geo-location.groovy.location").exists().given(applicationContext.getEnvironment()))
                .supply(() -> {
                    val resource = new WatchableGroovyScriptResource(casProperties.getGeoLocation().getGroovy().getLocation());
                    return new GroovyGeoLocationService(resource, applicationContext);
                })
                .otherwiseProxy()
                .get();
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "groovyGeoLocationServiceConfigurer")
        public GeoLocationServiceConfigurer groovyGeoLocationServiceConfigurer(
            @Qualifier("groovyGeoLocationService")
            final GeoLocationService groovyGeoLocationService) {
            return () -> groovyGeoLocationService;
        }
    }
}
