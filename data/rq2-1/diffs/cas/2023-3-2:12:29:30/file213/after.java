package org.apereo.cas.logout.config;

import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.authentication.principal.ServiceFactoryConfigurer;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.logout.DefaultLogoutExecutionPlan;
import org.apereo.cas.logout.DefaultLogoutManager;
import org.apereo.cas.logout.DefaultLogoutRedirectionStrategy;
import org.apereo.cas.logout.DefaultSingleLogoutMessageCreator;
import org.apereo.cas.logout.LogoutExecutionPlan;
import org.apereo.cas.logout.LogoutExecutionPlanConfigurer;
import org.apereo.cas.logout.LogoutManager;
import org.apereo.cas.logout.LogoutRedirectionStrategy;
import org.apereo.cas.logout.LogoutWebApplicationServiceFactory;
import org.apereo.cas.logout.slo.ChainingSingleLogoutServiceLogoutUrlBuilder;
import org.apereo.cas.logout.slo.DefaultSingleLogoutRequestExecutor;
import org.apereo.cas.logout.slo.DefaultSingleLogoutServiceLogoutUrlBuilder;
import org.apereo.cas.logout.slo.DefaultSingleLogoutServiceMessageHandler;
import org.apereo.cas.logout.slo.SingleLogoutMessageCreator;
import org.apereo.cas.logout.slo.SingleLogoutRequestExecutor;
import org.apereo.cas.logout.slo.SingleLogoutServiceLogoutUrlBuilder;
import org.apereo.cas.logout.slo.SingleLogoutServiceLogoutUrlBuilderConfigurer;
import org.apereo.cas.logout.slo.SingleLogoutServiceMessageHandler;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.http.HttpClient;
import org.apereo.cas.util.spring.beans.BeanSupplier;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;
import org.apereo.cas.web.UrlValidator;
import org.apereo.cas.web.support.ArgumentExtractor;

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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is {@link CasCoreLogoutConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.Logout)
@AutoConfiguration
public class CasCoreLogoutConfiguration {

    @Configuration(value = "CasCoreLogoutUrlBuilderConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreLogoutUrlBuilderConfiguration {
        @ConditionalOnMissingBean(name = "singleLogoutServiceLogoutUrlBuilder")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public SingleLogoutServiceLogoutUrlBuilder singleLogoutServiceLogoutUrlBuilder(
            final List<SingleLogoutServiceLogoutUrlBuilderConfigurer> configurers) {
            val results = configurers
                .stream()
                .sorted(Comparator.comparing(SingleLogoutServiceLogoutUrlBuilderConfigurer::getOrder))
                .map(cfg -> {
                    val builder = cfg.configureBuilder();
                    LOGGER.trace("Configuring single logout url builder [{}]", builder.getName());
                    return builder;
                })
                .filter(BeanSupplier::isNotProxy)
                .map(SingleLogoutServiceLogoutUrlBuilder.class::cast)
                .sorted(Comparator.comparing(SingleLogoutServiceLogoutUrlBuilder::getOrder))
                .collect(Collectors.toList());
            return new ChainingSingleLogoutServiceLogoutUrlBuilder(results);
        }

        @ConditionalOnMissingBean(name = "defaultSingleLogoutServiceLogoutUrlBuilderConfigurer")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public SingleLogoutServiceLogoutUrlBuilderConfigurer defaultSingleLogoutServiceLogoutUrlBuilderConfigurer(
            @Qualifier(UrlValidator.BEAN_NAME)
            final UrlValidator urlValidator,
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager) {
            return () -> new DefaultSingleLogoutServiceLogoutUrlBuilder(servicesManager, urlValidator);
        }
    }

    @Configuration(value = "CasCoreLogoutRedirectConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreLogoutRedirectConfiguration {
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "defaultLogoutRedirectionStrategy")
        public LogoutRedirectionStrategy defaultLogoutRedirectionStrategy(
            final CasConfigurationProperties casProperties,
            @Qualifier(ArgumentExtractor.BEAN_NAME)
            final ArgumentExtractor argumentExtractor,
            @Qualifier(WebApplicationService.BEAN_NAME_FACTORY)
            final ServiceFactory<WebApplicationService> webApplicationServiceFactory,
            @Qualifier("singleLogoutServiceLogoutUrlBuilder")
            final SingleLogoutServiceLogoutUrlBuilder singleLogoutServiceLogoutUrlBuilder) {
            return new DefaultLogoutRedirectionStrategy(argumentExtractor,
                casProperties, singleLogoutServiceLogoutUrlBuilder, webApplicationServiceFactory);
        }
    }

    @Configuration(value = "CasCoreLogoutMessagesConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreLogoutMessagesConfiguration {
        @ConditionalOnMissingBean(name = "defaultSingleLogoutServiceMessageHandler")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public SingleLogoutServiceMessageHandler defaultSingleLogoutServiceMessageHandler(
            @Qualifier(AuthenticationServiceSelectionPlan.BEAN_NAME)
            final AuthenticationServiceSelectionPlan authenticationServiceSelectionPlan,
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager,
            final CasConfigurationProperties casProperties,
            @Qualifier("defaultSingleLogoutMessageCreator")
            final SingleLogoutMessageCreator defaultSingleLogoutMessageCreator,
            @Qualifier(HttpClient.BEAN_NAME_HTTPCLIENT_NO_REDIRECT)
            final HttpClient noRedirectHttpClient,
            @Qualifier("singleLogoutServiceLogoutUrlBuilder")
            final SingleLogoutServiceLogoutUrlBuilder singleLogoutServiceLogoutUrlBuilder) {
            return new DefaultSingleLogoutServiceMessageHandler(noRedirectHttpClient,
                defaultSingleLogoutMessageCreator,
                servicesManager,
                singleLogoutServiceLogoutUrlBuilder,
                casProperties.getSlo().isAsynchronous(),
                authenticationServiceSelectionPlan);
        }

        @ConditionalOnMissingBean(name = "defaultSingleLogoutMessageCreator")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public SingleLogoutMessageCreator defaultSingleLogoutMessageCreator() {
            return new DefaultSingleLogoutMessageCreator();
        }
    }

    @Configuration(value = "CasCoreLogoutManagementConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreLogoutManagementConfiguration {

        @ConditionalOnMissingBean(name = LogoutManager.DEFAULT_BEAN_NAME)
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @Bean
        public LogoutManager logoutManager(
            @Qualifier(LogoutExecutionPlan.BEAN_NAME)
            final LogoutExecutionPlan logoutExecutionPlan,
            final CasConfigurationProperties casProperties) {
            return new DefaultLogoutManager(casProperties.getSlo().isDisabled(), logoutExecutionPlan);
        }

    }

    @Configuration(value = "CasCoreLogoutExecutionPlanBaseConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreLogoutExecutionPlanBaseConfiguration {
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "casCoreLogoutExecutionPlanConfigurer")
        public LogoutExecutionPlanConfigurer casCoreLogoutExecutionPlanConfigurer(
            final CasConfigurationProperties casProperties,
            @Qualifier("defaultSingleLogoutServiceMessageHandler")
            final SingleLogoutServiceMessageHandler defaultSingleLogoutServiceMessageHandler,
            @Qualifier("defaultLogoutRedirectionStrategy")
            final LogoutRedirectionStrategy defaultLogoutRedirectionStrategy,
            @Qualifier(TicketRegistry.BEAN_NAME)
            final TicketRegistry ticketRegistry) {
            return plan -> {
                plan.registerSingleLogoutServiceMessageHandler(defaultSingleLogoutServiceMessageHandler);
                plan.registerLogoutRedirectionStrategy(defaultLogoutRedirectionStrategy);

                if (casProperties.getLogout().isRemoveDescendantTickets()) {
                    LOGGER.debug("CAS is configured to remove descendant tickets of the ticket-granting tickets");
                    plan.registerLogoutPostProcessor(tgt -> tgt.getDescendantTickets().forEach(Unchecked.consumer(t -> {
                        LOGGER.debug("Deleting ticket [{}] from the registry as a descendant of [{}]", t, tgt.getId());
                        ticketRegistry.deleteTicket(t);
                    })));
                }
            };
        }
    }

    @Configuration(value = "CasCoreLogoutExecutionPlanConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreLogoutExecutionPlanConfiguration {
        @ConditionalOnMissingBean(name = LogoutExecutionPlan.BEAN_NAME)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public LogoutExecutionPlan logoutExecutionPlan(final List<LogoutExecutionPlanConfigurer> configurers) {
            val plan = new DefaultLogoutExecutionPlan();
            configurers.forEach(c -> {
                LOGGER.trace("Configuring logout execution plan [{}]", c.getName());
                c.configureLogoutExecutionPlan(plan);
            });
            return plan;
        }
    }

    @Configuration(value = "CasCoreLogoutServiceConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
    public static class CasCoreLogoutServiceConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "logoutWebApplicationServiceFactory")
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public ServiceFactory<WebApplicationService> logoutWebApplicationServiceFactory(final CasConfigurationProperties casProperties) {
            return new LogoutWebApplicationServiceFactory(casProperties.getLogout());
        }

        @Bean
        @ConditionalOnMissingBean(name = "logoutWebApplicationServiceFactoryConfigurer")
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public ServiceFactoryConfigurer logoutWebApplicationServiceFactoryConfigurer(
            @Qualifier("logoutWebApplicationServiceFactory")
            final ServiceFactory<WebApplicationService> logoutWebApplicationServiceFactory) {
            return () -> CollectionUtils.wrap(logoutWebApplicationServiceFactory);
        }
    }

    @Configuration(value = "CasCoreLogoutExecutorConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreLogoutExecutorConfiguration {
        @ConditionalOnMissingBean(name = SingleLogoutRequestExecutor.BEAN_NAME)
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @Bean
        public SingleLogoutRequestExecutor defaultSingleLogoutRequestExecutor(
            @Qualifier(TicketRegistry.BEAN_NAME)
            final TicketRegistry ticketRegistry,
            @Qualifier(LogoutManager.DEFAULT_BEAN_NAME)
            final LogoutManager logoutManager,
            final ConfigurableApplicationContext applicationContext) {
            return new DefaultSingleLogoutRequestExecutor(ticketRegistry, logoutManager, applicationContext);
        }
    }
}
