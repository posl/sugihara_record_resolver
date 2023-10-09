package org.apereo.cas.config;

import org.apereo.cas.api.AuthenticationRequestRiskCalculator;
import org.apereo.cas.api.AuthenticationRiskContingencyPlan;
import org.apereo.cas.api.AuthenticationRiskEvaluator;
import org.apereo.cas.api.AuthenticationRiskMitigator;
import org.apereo.cas.api.AuthenticationRiskNotifier;
import org.apereo.cas.audit.AuditActionResolvers;
import org.apereo.cas.audit.AuditResourceResolvers;
import org.apereo.cas.audit.AuditTrailRecordResolutionPlanConfigurer;
import org.apereo.cas.authentication.adaptive.geo.GeoLocationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.impl.calcs.DateTimeAuthenticationRequestRiskCalculator;
import org.apereo.cas.impl.calcs.GeoLocationAuthenticationRequestRiskCalculator;
import org.apereo.cas.impl.calcs.IpAddressAuthenticationRequestRiskCalculator;
import org.apereo.cas.impl.calcs.UserAgentAuthenticationRequestRiskCalculator;
import org.apereo.cas.impl.engine.DefaultAuthenticationRiskEvaluator;
import org.apereo.cas.impl.engine.DefaultAuthenticationRiskMitigator;
import org.apereo.cas.impl.notify.AuthenticationRiskEmailNotifier;
import org.apereo.cas.impl.notify.AuthenticationRiskSmsNotifier;
import org.apereo.cas.impl.plans.BaseAuthenticationRiskContingencyPlan;
import org.apereo.cas.impl.plans.BlockAuthenticationContingencyPlan;
import org.apereo.cas.impl.plans.MultifactorAuthenticationContingencyPlan;
import org.apereo.cas.notifications.CommunicationsManager;
import org.apereo.cas.support.events.CasEventRepository;
import org.apereo.cas.util.spring.beans.BeanCondition;
import org.apereo.cas.util.spring.beans.BeanSupplier;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import lombok.val;
import org.apereo.inspektr.audit.spi.AuditResourceResolver;
import org.apereo.inspektr.audit.spi.support.DefaultAuditActionResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

/**
 * This is {@link ElectronicFenceConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@EnableScheduling
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.Electrofence)
@AutoConfiguration
public class ElectronicFenceConfiguration {

    @Configuration(value = "ElectronicFenceMitigatorConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class ElectronicFenceMitigatorConfiguration {

        @ConditionalOnMissingBean(name = "authenticationRiskMitigator")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuthenticationRiskMitigator authenticationRiskMitigator(
            final CasConfigurationProperties casProperties,
            @Qualifier("blockAuthenticationContingencyPlan")
            final AuthenticationRiskContingencyPlan blockAuthenticationContingencyPlan,
            @Qualifier("multifactorAuthenticationContingencyPlan")
            final AuthenticationRiskContingencyPlan multifactorAuthenticationContingencyPlan) {
            if (casProperties.getAuthn().getAdaptive().getRisk().getResponse().isBlockAttempt()) {
                return new DefaultAuthenticationRiskMitigator(blockAuthenticationContingencyPlan);
            }
            return new DefaultAuthenticationRiskMitigator(multifactorAuthenticationContingencyPlan);
        }
    }

    @Configuration(value = "ElectronicFenceEvaluatorConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class ElectronicFenceEvaluatorConfiguration {
        @ConditionalOnMissingBean(name = "authenticationRiskEvaluator")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuthenticationRiskEvaluator authenticationRiskEvaluator(
            final List<AuthenticationRequestRiskCalculator> ipAddressAuthenticationRequestRiskCalculators) {
            return new DefaultAuthenticationRiskEvaluator(ipAddressAuthenticationRequestRiskCalculators);
        }
    }

    @Configuration(value = "ElectronicFenceContingencyConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class ElectronicFenceContingencyConfiguration {

        private static void configureContingencyPlan(final BaseAuthenticationRiskContingencyPlan plan,
                                                     final CasConfigurationProperties casProperties,
                                                     final AuthenticationRiskNotifier authenticationRiskEmailNotifier,
                                                     final AuthenticationRiskNotifier authenticationRiskSmsNotifier) {
            val response = casProperties.getAuthn().getAdaptive().getRisk().getResponse();
            val mail = response.getMail();
            if (mail.isDefined()) {
                plan.getNotifiers().add(authenticationRiskEmailNotifier);
            }
            val sms = response.getSms();
            if (sms.isDefined()) {
                plan.getNotifiers().add(authenticationRiskSmsNotifier);
            }
        }

        @ConditionalOnMissingBean(name = "blockAuthenticationContingencyPlan")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuthenticationRiskContingencyPlan blockAuthenticationContingencyPlan(
            final CasConfigurationProperties casProperties,
            final ConfigurableApplicationContext applicationContext,
            @Qualifier("authenticationRiskEmailNotifier")
            final AuthenticationRiskNotifier authenticationRiskEmailNotifier,
            @Qualifier("authenticationRiskSmsNotifier")
            final AuthenticationRiskNotifier authenticationRiskSmsNotifier) {
            val plan = new BlockAuthenticationContingencyPlan(casProperties, applicationContext);
            configureContingencyPlan(plan, casProperties, authenticationRiskEmailNotifier, authenticationRiskSmsNotifier);
            return plan;
        }

        @ConditionalOnMissingBean(name = "multifactorAuthenticationContingencyPlan")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuthenticationRiskContingencyPlan multifactorAuthenticationContingencyPlan(
            final CasConfigurationProperties casProperties,
            final ConfigurableApplicationContext applicationContext,
            @Qualifier("authenticationRiskEmailNotifier")
            final AuthenticationRiskNotifier authenticationRiskEmailNotifier,
            @Qualifier("authenticationRiskSmsNotifier")
            final AuthenticationRiskNotifier authenticationRiskSmsNotifier) {
            val plan = new MultifactorAuthenticationContingencyPlan(casProperties, applicationContext);
            configureContingencyPlan(plan, casProperties, authenticationRiskEmailNotifier, authenticationRiskSmsNotifier);
            return plan;
        }
    }

    @Configuration(value = "ElectronicFenceNotifierConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class ElectronicFenceNotifierConfiguration {

        @ConditionalOnMissingBean(name = "authenticationRiskEmailNotifier")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuthenticationRiskNotifier authenticationRiskEmailNotifier(
            final CasConfigurationProperties casProperties,
            @Qualifier(CommunicationsManager.BEAN_NAME)
            final CommunicationsManager communicationsManager) {
            return new AuthenticationRiskEmailNotifier(casProperties, communicationsManager);
        }

        @ConditionalOnMissingBean(name = "authenticationRiskSmsNotifier")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuthenticationRiskNotifier authenticationRiskSmsNotifier(
            final CasConfigurationProperties casProperties,
            @Qualifier(CommunicationsManager.BEAN_NAME)
            final CommunicationsManager communicationsManager) {
            return new AuthenticationRiskSmsNotifier(casProperties, communicationsManager);
        }

    }

    @Configuration(value = "ElectronicFenceCalculatorConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class ElectronicFenceCalculatorConfiguration {

        @ConditionalOnMissingBean(name = "ipAddressAuthenticationRequestRiskCalculator")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuthenticationRequestRiskCalculator ipAddressAuthenticationRequestRiskCalculator(
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties,
            @Qualifier(CasEventRepository.BEAN_NAME)
            final CasEventRepository casEventRepository) throws Exception {
            return BeanSupplier.of(AuthenticationRequestRiskCalculator.class)
                .when(BeanCondition.on("cas.authn.adaptive.risk.ip.enabled").isTrue().given(applicationContext.getEnvironment()))
                .supply(() -> new IpAddressAuthenticationRequestRiskCalculator(casEventRepository, casProperties))
                .otherwiseProxy()
                .get();
        }

        @ConditionalOnMissingBean(name = "userAgentAuthenticationRequestRiskCalculator")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuthenticationRequestRiskCalculator userAgentAuthenticationRequestRiskCalculator(
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties,
            @Qualifier(CasEventRepository.BEAN_NAME)
            final CasEventRepository casEventRepository) throws Exception {
            return BeanSupplier.of(AuthenticationRequestRiskCalculator.class)
                .when(BeanCondition.on("cas.authn.adaptive.risk.agent.enabled").isTrue().given(applicationContext.getEnvironment()))
                .supply(() -> new UserAgentAuthenticationRequestRiskCalculator(casEventRepository, casProperties))
                .otherwiseProxy()
                .get();
        }

        @ConditionalOnMissingBean(name = "dateTimeAuthenticationRequestRiskCalculator")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuthenticationRequestRiskCalculator dateTimeAuthenticationRequestRiskCalculator(
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties,
            @Qualifier(CasEventRepository.BEAN_NAME)
            final CasEventRepository casEventRepository) throws Exception {
            return BeanSupplier.of(AuthenticationRequestRiskCalculator.class)
                .when(BeanCondition.on("cas.authn.adaptive.risk.date-time.enabled").isTrue().given(applicationContext.getEnvironment()))
                .supply(() -> new DateTimeAuthenticationRequestRiskCalculator(casEventRepository, casProperties))
                .otherwiseProxy()
                .get();
        }
    }

    @ConditionalOnBean(name = GeoLocationService.BEAN_NAME)
    @Configuration(value = "ElectronicFenceGeoLocationConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class ElectronicFenceGeoLocationConfiguration {
        @ConditionalOnMissingBean(name = "geoLocationAuthenticationRequestRiskCalculator")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuthenticationRequestRiskCalculator geoLocationAuthenticationRequestRiskCalculator(
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties,
            @Qualifier(GeoLocationService.BEAN_NAME)
            final GeoLocationService geoLocationService,
            @Qualifier(CasEventRepository.BEAN_NAME)
            final CasEventRepository casEventRepository) throws Exception {
            return BeanSupplier.of(AuthenticationRequestRiskCalculator.class)
                .when(BeanCondition.on("cas.authn.adaptive.risk.geo-location.enabled").isTrue().given(applicationContext.getEnvironment()))
                .supply(() -> new GeoLocationAuthenticationRequestRiskCalculator(casEventRepository, casProperties, geoLocationService))
                .otherwiseProxy()
                .get();
        }
    }

    @Configuration(value = "ElectronicFenceAuditConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class ElectronicFenceAuditConfiguration {
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "casElectrofenceAuditTrailRecordResolutionPlanConfigurer")
        public AuditTrailRecordResolutionPlanConfigurer casElectrofenceAuditTrailRecordResolutionPlanConfigurer(
            @Qualifier("returnValueResourceResolver")
            final AuditResourceResolver returnValueResourceResolver) {
            return plan -> {
                plan.registerAuditActionResolver(AuditActionResolvers.ADAPTIVE_RISKY_AUTHENTICATION_ACTION_RESOLVER,
                    new DefaultAuditActionResolver());
                plan.registerAuditResourceResolver(AuditResourceResolvers.ADAPTIVE_RISKY_AUTHENTICATION_RESOURCE_RESOLVER,
                    returnValueResourceResolver);
            };
        }
    }
}
