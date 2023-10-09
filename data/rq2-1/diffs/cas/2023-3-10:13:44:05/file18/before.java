package org.apereo.cas.config;

import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.PseudoPlatformTransactionManager;
import org.apereo.cas.authentication.policy.UniquePrincipalAuthenticationPolicy;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.logout.LogoutManager;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.ticket.DefaultServiceTicketSessionTrackingPolicy;
import org.apereo.cas.ticket.DefaultTicketCatalog;
import org.apereo.cas.ticket.ExpirationPolicyBuilder;
import org.apereo.cas.ticket.ServiceTicketFactory;
import org.apereo.cas.ticket.ServiceTicketGeneratorAuthority;
import org.apereo.cas.ticket.ServiceTicketSessionTrackingPolicy;
import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.TicketCatalog;
import org.apereo.cas.ticket.TicketCatalogConfigurer;
import org.apereo.cas.ticket.TicketFactory;
import org.apereo.cas.ticket.TicketFactoryExecutionPlanConfigurer;
import org.apereo.cas.ticket.TicketGrantingTicketFactory;
import org.apereo.cas.ticket.TransientSessionTicketFactory;
import org.apereo.cas.ticket.UniqueTicketIdGenerator;
import org.apereo.cas.ticket.expiration.builder.ProxyGrantingTicketExpirationPolicyBuilder;
import org.apereo.cas.ticket.expiration.builder.ProxyTicketExpirationPolicyBuilder;
import org.apereo.cas.ticket.expiration.builder.ServiceTicketExpirationPolicyBuilder;
import org.apereo.cas.ticket.expiration.builder.TicketGrantingTicketExpirationPolicyBuilder;
import org.apereo.cas.ticket.expiration.builder.TransientSessionTicketExpirationPolicyBuilder;
import org.apereo.cas.ticket.factory.DefaultProxyGrantingTicketFactory;
import org.apereo.cas.ticket.factory.DefaultProxyTicketFactory;
import org.apereo.cas.ticket.factory.DefaultServiceTicketFactory;
import org.apereo.cas.ticket.factory.DefaultTicketFactory;
import org.apereo.cas.ticket.factory.DefaultTicketGrantingTicketFactory;
import org.apereo.cas.ticket.factory.DefaultTransientSessionTicketFactory;
import org.apereo.cas.ticket.proxy.ProxyGrantingTicketFactory;
import org.apereo.cas.ticket.proxy.ProxyTicketFactory;
import org.apereo.cas.ticket.registry.CachingTicketRegistry;
import org.apereo.cas.ticket.registry.DefaultTicketRegistry;
import org.apereo.cas.ticket.registry.DefaultTicketRegistrySupport;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.apereo.cas.ticket.serialization.TicketSerializationManager;
import org.apereo.cas.util.CoreTicketUtils;
import org.apereo.cas.util.ProxyGrantingTicketIdGenerator;
import org.apereo.cas.util.ProxyTicketIdGenerator;
import org.apereo.cas.util.TicketGrantingTicketIdGenerator;
import org.apereo.cas.util.cipher.CipherExecutorUtils;
import org.apereo.cas.util.cipher.ProtocolTicketCipherExecutor;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.util.lock.LockRepository;
import org.apereo.cas.util.spring.beans.BeanCondition;
import org.apereo.cas.util.spring.beans.BeanSupplier;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is {@link CasCoreTicketsConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@EnableScheduling
@EnableAsync(proxyTargetClass = false)
@EnableAspectJAutoProxy(proxyTargetClass = false)
@Slf4j
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.TicketRegistry)
@AutoConfiguration
public class CasCoreTicketsConfiguration {
    @Configuration(value = "CasCoreTicketsBaseConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreTicketsBaseConfiguration {

        @ConditionalOnMissingBean(name = ServiceTicketSessionTrackingPolicy.BEAN_NAME)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public ServiceTicketSessionTrackingPolicy serviceTicketSessionTrackingPolicy(
            @Qualifier(TicketRegistry.BEAN_NAME) final TicketRegistry ticketRegistry,
            final CasConfigurationProperties casProperties) {
            return new DefaultServiceTicketSessionTrackingPolicy(casProperties, ticketRegistry);
        }

        @ConditionalOnMissingBean(name = TicketRegistrySupport.BEAN_NAME)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public TicketRegistrySupport defaultTicketRegistrySupport(
            @Qualifier(TicketRegistry.BEAN_NAME) final TicketRegistry ticketRegistry) {
            return new DefaultTicketRegistrySupport(ticketRegistry);
        }
    }

    @Configuration(value = "CasCoreTicketsAuthenticationPlanConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreTicketsAuthenticationPlanConfiguration {
        @ConditionalOnMissingBean(name = "ticketAuthenticationPolicyExecutionPlanConfigurer")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AuthenticationEventExecutionPlanConfigurer ticketAuthenticationPolicyExecutionPlanConfigurer(
            @Qualifier(TicketRegistry.BEAN_NAME) final TicketRegistry ticketRegistry,
            final CasConfigurationProperties casProperties) {
            return plan -> {
                val policyProps = casProperties.getAuthn().getPolicy();
                if (policyProps.getUniquePrincipal().isEnabled()) {
                    LOGGER.trace("Activating authentication policy [{}]", UniquePrincipalAuthenticationPolicy.class.getSimpleName());
                    plan.registerAuthenticationPolicy(new UniquePrincipalAuthenticationPolicy(ticketRegistry));
                }
            };
        }
    }

    @Configuration(value = "CasCoreTicketRegistryConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreTicketRegistryConfiguration {

        @ConditionalOnMissingBean(name = TicketRegistry.BEAN_NAME)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public TicketRegistry ticketRegistry(
            @Qualifier(TicketCatalog.BEAN_NAME)
            final TicketCatalog ticketCatalog,
            @Qualifier(TicketSerializationManager.BEAN_NAME)
            final TicketSerializationManager ticketSerializationManager,
            @Qualifier(LogoutManager.DEFAULT_BEAN_NAME)
            final ObjectProvider<LogoutManager> logoutManager,
            final CasConfigurationProperties casProperties) {
            LOGGER.info("Runtime memory is used as the persistence storage for retrieving and managing tickets. "
                        + "Tickets that are issued during runtime will be LOST when the web server is restarted. This MAY impact SSO functionality.");
            val mem = casProperties.getTicket().getRegistry().getInMemory();
            val cipher = CoreTicketUtils.newTicketRegistryCipherExecutor(mem.getCrypto(), "in-memory");

            if (mem.isCache()) {
                return new CachingTicketRegistry(cipher, ticketSerializationManager, ticketCatalog, logoutManager);
            }
            val storageMap = new ConcurrentHashMap<String, Ticket>(mem.getInitialCapacity(), mem.getLoadFactor(), mem.getConcurrency());
            return new DefaultTicketRegistry(cipher, ticketSerializationManager, ticketCatalog, storageMap);
        }

        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @Bean
        @ConditionalOnMissingBean(name = "protocolTicketCipherExecutor")
        public CipherExecutor protocolTicketCipherExecutor(
            final CasConfigurationProperties casProperties) {
            val crypto = casProperties.getTicket().getCrypto();
            if (crypto.isEnabled()) {
                return CipherExecutorUtils.newStringCipherExecutor(crypto, ProtocolTicketCipherExecutor.class);
            }
            LOGGER.trace("Protocol tickets generated by CAS are not signed/encrypted.");
            return CipherExecutor.noOp();
        }

        @ConditionalOnMissingBean(name = TicketCatalog.BEAN_NAME)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public TicketCatalog ticketCatalog(
            final CasConfigurationProperties casProperties,
            final List<TicketCatalogConfigurer> configurers) {
            val plan = new DefaultTicketCatalog();
            configurers.forEach(c -> {
                LOGGER.trace("Configuring ticket metadata registration plan [{}]", c.getName());
                c.configureTicketCatalog(plan, casProperties);
            });
            return plan;
        }
    }

    @Configuration(value = "CasCoreTicketIdGeneratorConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreTicketIdGeneratorConfiguration {
        @ConditionalOnMissingBean(name = "proxyGrantingTicketUniqueIdGenerator")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public UniqueTicketIdGenerator proxyGrantingTicketUniqueIdGenerator(
            final CasConfigurationProperties casProperties) {
            return new ProxyGrantingTicketIdGenerator(
                casProperties.getTicket().getTgt().getCore().getMaxLength(),
                casProperties.getHost().getName());
        }

        @ConditionalOnMissingBean(name = "ticketGrantingTicketUniqueIdGenerator")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public UniqueTicketIdGenerator ticketGrantingTicketUniqueIdGenerator(
            final CasConfigurationProperties casProperties) {
            return new TicketGrantingTicketIdGenerator(
                casProperties.getTicket().getTgt().getCore().getMaxLength(),
                casProperties.getHost().getName());
        }

        @ConditionalOnMissingBean(name = "proxy20TicketUniqueIdGenerator")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public UniqueTicketIdGenerator proxy20TicketUniqueIdGenerator(
            final CasConfigurationProperties casProperties) {
            return new ProxyTicketIdGenerator(
                casProperties.getTicket().getPgt().getMaxLength(),
                casProperties.getHost().getName());
        }
    }

    @Configuration(value = "CasCoreProxyGrantingTicketExecutionPlanConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreProxyGrantingTicketExecutionPlanConfiguration {
        @ConditionalOnMissingBean(name = "defaultProxyGrantingTicketFactoryConfigurer")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public TicketFactoryExecutionPlanConfigurer defaultProxyGrantingTicketFactoryConfigurer(
            @Qualifier("defaultProxyGrantingTicketFactory") final ProxyGrantingTicketFactory defaultProxyGrantingTicketFactory) {
            return () -> defaultProxyGrantingTicketFactory;
        }
    }

    @Configuration(value = "CasCoreProxyTicketExecutionPlanConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreProxyTicketExecutionPlanConfiguration {
        @ConditionalOnMissingBean(name = "defaultProxyTicketFactoryConfigurer")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public TicketFactoryExecutionPlanConfigurer defaultProxyTicketFactoryConfigurer(
            @Qualifier("defaultProxyTicketFactory") final ProxyTicketFactory defaultProxyTicketFactory) {
            return () -> defaultProxyTicketFactory;
        }
    }

    @Configuration(value = "CasCoreServiceTicketExecutionPlanConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreServiceTicketExecutionPlanConfiguration {
        @ConditionalOnMissingBean(name = "defaultServiceTicketFactoryConfigurer")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public TicketFactoryExecutionPlanConfigurer defaultServiceTicketFactoryConfigurer(
            @Qualifier("defaultServiceTicketFactory") final ServiceTicketFactory defaultServiceTicketFactory) {
            return () -> defaultServiceTicketFactory;
        }
    }

    @Configuration(value = "CasCoreTransientSessionTicketExecutionPlanConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreTransientSessionTicketExecutionPlanConfiguration {
        @ConditionalOnMissingBean(name = "defaultTransientSessionTicketFactoryConfigurer")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public TicketFactoryExecutionPlanConfigurer defaultTransientSessionTicketFactoryConfigurer(
            @Qualifier("defaultTransientSessionTicketFactory") final TransientSessionTicketFactory defaultTransientSessionTicketFactory) {
            return () -> defaultTransientSessionTicketFactory;
        }
    }

    @Configuration(value = "CasCoreTicketGrantingTicketExecutionPlanConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreTicketGrantingTicketExecutionPlanConfiguration {
        @ConditionalOnMissingBean(name = "defaultTicketGrantingTicketFactoryConfigurer")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public TicketFactoryExecutionPlanConfigurer defaultTicketGrantingTicketFactoryConfigurer(
            @Qualifier("defaultTicketGrantingTicketFactory") final TicketGrantingTicketFactory defaultTicketGrantingTicketFactory) {
            return () -> defaultTicketGrantingTicketFactory;
        }
    }

    @Configuration(value = "CasCoreTicketGrantingTicketFactoryConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreTicketGrantingTicketFactoryConfiguration {
        @ConditionalOnMissingBean(name = "defaultTicketGrantingTicketFactory")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public TicketGrantingTicketFactory defaultTicketGrantingTicketFactory(
            @Qualifier(ExpirationPolicyBuilder.BEAN_NAME_TICKET_GRANTING_TICKET_EXPIRATION_POLICY) final ExpirationPolicyBuilder grantingTicketExpirationPolicy,
            @Qualifier("protocolTicketCipherExecutor") final CipherExecutor protocolTicketCipherExecutor,
            @Qualifier("ticketGrantingTicketUniqueIdGenerator") final UniqueTicketIdGenerator ticketGrantingTicketUniqueIdGenerator,
            @Qualifier(ServicesManager.BEAN_NAME) final ServicesManager servicesManager) {
            return new DefaultTicketGrantingTicketFactory(ticketGrantingTicketUniqueIdGenerator,
                grantingTicketExpirationPolicy, protocolTicketCipherExecutor, servicesManager);
        }

    }

    @Configuration(value = "CasCoreServiceTicketFactoryConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreServiceTicketFactoryConfiguration {
        @ConditionalOnMissingBean(name = "defaultServiceTicketFactory")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public ServiceTicketFactory defaultServiceTicketFactory(
            @Qualifier(ServiceTicketSessionTrackingPolicy.BEAN_NAME) final ServiceTicketSessionTrackingPolicy serviceTicketSessionTrackingPolicy,
            @Qualifier("protocolTicketCipherExecutor") final CipherExecutor protocolTicketCipherExecutor,
            @Qualifier(ExpirationPolicyBuilder.BEAN_NAME_SERVICE_TICKET_EXPIRATION_POLICY) final ExpirationPolicyBuilder serviceTicketExpirationPolicy,
            @Qualifier(ServicesManager.BEAN_NAME) final ServicesManager servicesManager,
            @Qualifier("uniqueIdGeneratorsMap") final Map<String, UniqueTicketIdGenerator> uniqueIdGeneratorsMap) {
            return new DefaultServiceTicketFactory(serviceTicketExpirationPolicy,
                uniqueIdGeneratorsMap, serviceTicketSessionTrackingPolicy,
                protocolTicketCipherExecutor, servicesManager);
        }

        @ConditionalOnMissingBean(name = "defaultServiceTicketGeneratorAuthority")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public ServiceTicketGeneratorAuthority defaultServiceTicketGeneratorAuthority() {
            return ServiceTicketGeneratorAuthority.allow();
        }
    }

    @Configuration(value = "CasCoreProxyTicketFactoryConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreProxyTicketFactoryConfiguration {
        @ConditionalOnMissingBean(name = "defaultProxyTicketFactory")
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @Bean
        public ProxyTicketFactory defaultProxyTicketFactory(
            @Qualifier(ServiceTicketSessionTrackingPolicy.BEAN_NAME) final ServiceTicketSessionTrackingPolicy serviceTicketSessionTrackingPolicy,
            @Qualifier("protocolTicketCipherExecutor") final CipherExecutor protocolTicketCipherExecutor,
            @Qualifier(ExpirationPolicyBuilder.BEAN_NAME_PROXY_TICKET_EXPIRATION_POLICY) final ExpirationPolicyBuilder proxyTicketExpirationPolicy,
            @Qualifier("uniqueIdGeneratorsMap") final Map<String, UniqueTicketIdGenerator> uniqueIdGeneratorsMap,
            @Qualifier(ServicesManager.BEAN_NAME) final ServicesManager servicesManager) {
            return new DefaultProxyTicketFactory(proxyTicketExpirationPolicy, uniqueIdGeneratorsMap,
                protocolTicketCipherExecutor, serviceTicketSessionTrackingPolicy, servicesManager);
        }
    }

    @Configuration(value = "CasCoreTransientSessionTicketFactoryConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreTransientSessionTicketFactoryConfiguration {

        @ConditionalOnMissingBean(name = "defaultTransientSessionTicketFactory")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public TransientSessionTicketFactory defaultTransientSessionTicketFactory(
            @Qualifier(ExpirationPolicyBuilder.BEAN_NAME_TRANSIENT_SESSION_TICKET_EXPIRATION_POLICY) final ExpirationPolicyBuilder transientSessionTicketExpirationPolicy) {
            return new DefaultTransientSessionTicketFactory(transientSessionTicketExpirationPolicy);
        }
    }

    @Configuration(value = "CasCoreProxyGrantingTicketFactoryConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreProxyGrantingTicketFactoryConfiguration {
        @ConditionalOnMissingBean(name = "defaultProxyGrantingTicketFactory")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public ProxyGrantingTicketFactory defaultProxyGrantingTicketFactory(
            @Qualifier(ExpirationPolicyBuilder.BEAN_NAME_PROXY_GRANTING_TICKET_EXPIRATION_POLICY) final ExpirationPolicyBuilder proxyGrantingTicketExpirationPolicy,
            @Qualifier("proxyGrantingTicketUniqueIdGenerator") final UniqueTicketIdGenerator proxyGrantingTicketUniqueIdGenerator,
            @Qualifier("protocolTicketCipherExecutor") final CipherExecutor protocolTicketCipherExecutor,
            @Qualifier(ServicesManager.BEAN_NAME) final ServicesManager servicesManager) {
            return new DefaultProxyGrantingTicketFactory(
                proxyGrantingTicketUniqueIdGenerator,
                proxyGrantingTicketExpirationPolicy,
                protocolTicketCipherExecutor,
                servicesManager);
        }
    }

    @Configuration(value = "CasCoreTicketPlanConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreTicketPlanConfiguration {
        @ConditionalOnMissingBean(name = TicketFactory.BEAN_NAME)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public TicketFactory defaultTicketFactory(final List<TicketFactoryExecutionPlanConfigurer> configurers) {
            val parentFactory = new DefaultTicketFactory();
            configurers.forEach(configurer -> {
                val factory = configurer.configureTicketFactory();
                LOGGER.trace("Registering ticket factory via [{}]", factory.getName());
                parentFactory.addTicketFactory(factory.getTicketType(), factory);
            });
            return parentFactory;
        }
    }

    @Configuration(value = "CasCoreTicketExpirationPolicyConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreTicketExpirationPolicyConfiguration {
        @ConditionalOnMissingBean(name = ExpirationPolicyBuilder.BEAN_NAME_TRANSIENT_SESSION_TICKET_EXPIRATION_POLICY)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public ExpirationPolicyBuilder transientSessionTicketExpirationPolicy(
            final CasConfigurationProperties casProperties) {
            return new TransientSessionTicketExpirationPolicyBuilder(casProperties);
        }

        @ConditionalOnMissingBean(name = ExpirationPolicyBuilder.BEAN_NAME_TICKET_GRANTING_TICKET_EXPIRATION_POLICY)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public ExpirationPolicyBuilder grantingTicketExpirationPolicy(
            final CasConfigurationProperties casProperties) {
            return new TicketGrantingTicketExpirationPolicyBuilder(casProperties);
        }

        @ConditionalOnMissingBean(name = ExpirationPolicyBuilder.BEAN_NAME_PROXY_GRANTING_TICKET_EXPIRATION_POLICY)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public ExpirationPolicyBuilder proxyGrantingTicketExpirationPolicy(final CasConfigurationProperties casProperties) {
            val grantingTicketExpirationPolicy = new TicketGrantingTicketExpirationPolicyBuilder(casProperties);
            return new ProxyGrantingTicketExpirationPolicyBuilder(grantingTicketExpirationPolicy, casProperties);
        }

        @ConditionalOnMissingBean(name = ExpirationPolicyBuilder.BEAN_NAME_SERVICE_TICKET_EXPIRATION_POLICY)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public ExpirationPolicyBuilder serviceTicketExpirationPolicy(
            final CasConfigurationProperties casProperties) {
            return new ServiceTicketExpirationPolicyBuilder(casProperties);
        }

        @ConditionalOnMissingBean(name = ExpirationPolicyBuilder.BEAN_NAME_PROXY_TICKET_EXPIRATION_POLICY)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public ExpirationPolicyBuilder proxyTicketExpirationPolicy(
            final CasConfigurationProperties casProperties) {
            return new ProxyTicketExpirationPolicyBuilder(casProperties);
        }
    }

    @Configuration(value = "CasCoreTicketTransactionConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    @EnableTransactionManagement(proxyTargetClass = false)
    @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
    public static class CasCoreTicketTransactionConfiguration {
        @ConditionalOnMissingBean(name = "ticketTransactionManager")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public PlatformTransactionManager ticketTransactionManager() {
            return new PseudoPlatformTransactionManager();
        }
    }

    @Configuration(value = "CasCoreTicketLockingConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreTicketLockingConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = LockRepository.BEAN_NAME)
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public LockRepository casTicketRegistryLockRepository(
            final ConfigurableApplicationContext applicationContext) throws Exception {
            return BeanSupplier.of(LockRepository.class)
                .when(BeanCondition.on("cas.ticket.registry.core.enable-locking").isTrue().evenIfMissing().given(applicationContext.getEnvironment()))
                .supply(LockRepository::asDefault)
                .otherwise(LockRepository::noOp)
                .get();
        }
    }
}
