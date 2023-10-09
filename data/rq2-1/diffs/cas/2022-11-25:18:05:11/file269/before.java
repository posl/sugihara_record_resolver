package org.apereo.cas.config;

import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationManager;
import org.apereo.cas.authentication.AuthenticationResultBuilderFactory;
import org.apereo.cas.authentication.AuthenticationTransactionFactory;
import org.apereo.cas.authentication.AuthenticationTransactionManager;
import org.apereo.cas.authentication.DefaultAuthenticationAttributeReleasePolicy;
import org.apereo.cas.authentication.DefaultAuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.DefaultAuthenticationManager;
import org.apereo.cas.authentication.DefaultAuthenticationResultBuilderFactory;
import org.apereo.cas.authentication.DefaultAuthenticationTransactionFactory;
import org.apereo.cas.authentication.DefaultAuthenticationTransactionManager;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.util.model.TriStateBoolean;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;
import org.apereo.cas.validation.AuthenticationAttributeReleasePolicy;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.List;

/**
 * This is {@link CasCoreAuthenticationConfiguration}.
 *
 * @author Misagh Moayyed
 * @author Dmitriy Kopylenko
 * @since 5.0.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.Authentication)
@AutoConfiguration(after = CasCoreServicesConfiguration.class)
public class CasCoreAuthenticationConfiguration {

    @Configuration(value = "CasCoreAuthenticationBaseConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreAuthenticationBaseConfiguration {

        @ConditionalOnMissingBean(name = "authenticationResultBuilderFactory")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuthenticationResultBuilderFactory authenticationResultBuilderFactory() {
            return new DefaultAuthenticationResultBuilderFactory();
        }

        @ConditionalOnMissingBean(name = "authenticationTransactionFactory")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuthenticationTransactionFactory authenticationTransactionFactory() {
            return new DefaultAuthenticationTransactionFactory();
        }

        @ConditionalOnMissingBean(name = AuthenticationAttributeReleasePolicy.BEAN_NAME)
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @Bean
        public AuthenticationAttributeReleasePolicy authenticationAttributeReleasePolicy(
            final CasConfigurationProperties casProperties) {
            val release = casProperties.getAuthn().getAuthenticationAttributeRelease();
            if (!release.isEnabled()) {
                LOGGER.debug("CAS is configured to not release protocol-level authentication attributes.");
                return AuthenticationAttributeReleasePolicy.none();
            }
            return new DefaultAuthenticationAttributeReleasePolicy(release.getOnlyRelease(),
                release.getNeverRelease(),
                casProperties.getAuthn().getMfa().getCore().getAuthenticationContextAttribute());
        }
    }

    @Configuration(value = "CasCoreAuthenticationManagerConfiguration", proxyBeanMethods = false)
    @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
    public static class CasCoreAuthenticationManagerConfiguration {
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "authenticationTransactionManager")
        public AuthenticationTransactionManager authenticationTransactionManager(
            @Qualifier("casAuthenticationManager")
            final AuthenticationManager casAuthenticationManager,
            final ConfigurableApplicationContext applicationContext) {
            return new DefaultAuthenticationTransactionManager(applicationContext, casAuthenticationManager);
        }

        @ConditionalOnMissingBean(name = "casAuthenticationManager")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuthenticationManager casAuthenticationManager(
            final CasConfigurationProperties casProperties,
            final ConfigurableApplicationContext applicationContext,
            @Qualifier(AuthenticationEventExecutionPlan.DEFAULT_BEAN_NAME)
            final AuthenticationEventExecutionPlan authenticationEventExecutionPlan) {
            val isFatal = casProperties.getPersonDirectory().getPrincipalResolutionFailureFatal() == TriStateBoolean.TRUE;
            return new DefaultAuthenticationManager(authenticationEventExecutionPlan, isFatal, applicationContext);
        }
    }

    @Configuration(value = "CasCoreAuthenticationPlanConfiguration", proxyBeanMethods = false)
    @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
    public static class CasCoreAuthenticationPlanConfiguration {
        @ConditionalOnMissingBean(name = AuthenticationEventExecutionPlan.DEFAULT_BEAN_NAME)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuthenticationEventExecutionPlan authenticationEventExecutionPlan(
            final List<AuthenticationEventExecutionPlanConfigurer> configurers) {
            val plan = new DefaultAuthenticationEventExecutionPlan();
            val sortedConfigurers = new ArrayList<>(configurers);
            AnnotationAwareOrderComparator.sortIfNecessary(sortedConfigurers);

            sortedConfigurers.forEach(Unchecked.consumer(c -> {
                LOGGER.trace("Configuring authentication execution plan [{}]", c.getName());
                c.configureAuthenticationExecutionPlan(plan);
            }));
            return plan;
        }
    }
}
