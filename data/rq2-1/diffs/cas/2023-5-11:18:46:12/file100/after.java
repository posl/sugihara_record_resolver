package org.apereo.cas.config;

import org.apereo.cas.authentication.CoreAuthenticationUtils;
import org.apereo.cas.authentication.attribute.AttributeDefinitionStore;
import org.apereo.cas.authentication.attribute.AttributeDefinitionStoreConfigurer;
import org.apereo.cas.authentication.attribute.DefaultAttributeDefinitionStore;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.principal.PrincipalResolutionExecutionPlanConfigurer;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.configuration.support.Beans;
import org.apereo.cas.persondir.DefaultPersonDirectoryAttributeRepositoryPlan;
import org.apereo.cas.persondir.PersonDirectoryAttributeRepositoryCustomizer;
import org.apereo.cas.persondir.PersonDirectoryAttributeRepositoryPlan;
import org.apereo.cas.persondir.PersonDirectoryAttributeRepositoryPlanConfigurer;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.util.function.FunctionUtils;
import org.apereo.cas.util.spring.beans.BeanContainer;
import org.apereo.cas.util.spring.beans.BeanSupplier;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apereo.services.persondir.IPersonAttributeDao;
import org.apereo.services.persondir.support.AbstractAggregatingDefaultQueryPersonAttributeDao;
import org.apereo.services.persondir.support.CachingPersonAttributeDaoImpl;
import org.apereo.services.persondir.support.CascadingPersonAttributeDao;
import org.apereo.services.persondir.support.MergingPersonAttributeDaoImpl;
import org.apereo.services.persondir.support.merger.IAttributeMerger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This is {@link CasPersonDirectoryConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.PersonDirectory)
@AutoConfiguration
public class CasPersonDirectoryConfiguration {

    @Configuration(value = "CasPersonDirectoryAttributeDefinitionConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasPersonDirectoryAttributeDefinitionConfiguration {

        @ConditionalOnMissingBean(name = AttributeDefinitionStore.BEAN_NAME)
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public AttributeDefinitionStore attributeDefinitionStore(
            final ConfigurableApplicationContext applicationContext,
            final CasConfigurationProperties casProperties) throws Exception {
            val resource = casProperties.getAuthn().getAttributeRepository().getAttributeDefinitionStore().getJson().getLocation();
            val store = new DefaultAttributeDefinitionStore(resource);
            store.setScope(casProperties.getServer().getScope());
            val builders = applicationContext.getBeansOfType(AttributeDefinitionStoreConfigurer.class).values();
            builders
                .stream()
                .filter(BeanSupplier::isNotProxy)
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .forEach(cfg -> cfg.configure(store));
            return store;
        }
    }
    @Configuration(value = "CasPersonDirectoryPrincipalResolutionConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasPersonDirectoryPrincipalResolutionConfiguration {
        @ConditionalOnMissingBean(name = "personDirectoryPrincipalFactory")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public PrincipalFactory personDirectoryPrincipalFactory() {
            return PrincipalFactoryUtils.newPrincipalFactory();
        }

        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @Bean
        @ConditionalOnMissingBean(name = "personDirectoryAttributeRepositoryPrincipalResolver")
        public PrincipalResolver personDirectoryAttributeRepositoryPrincipalResolver(
            @Qualifier(AttributeDefinitionStore.BEAN_NAME)
            final AttributeDefinitionStore attributeDefinitionStore,
            @Qualifier(ServicesManager.BEAN_NAME)
            final ServicesManager servicesManager,
            @Qualifier("attributeRepositoryAttributeMerger")
            final IAttributeMerger attributeRepositoryAttributeMerger,
            final CasConfigurationProperties casProperties,
            @Qualifier("personDirectoryPrincipalFactory")
            final PrincipalFactory personDirectoryPrincipalFactory,
            @Qualifier(PrincipalResolver.BEAN_NAME_ATTRIBUTE_REPOSITORY)
            final IPersonAttributeDao attributeRepository) {
            val personDirectory = casProperties.getPersonDirectory();
            return CoreAuthenticationUtils.newPersonDirectoryPrincipalResolver(personDirectoryPrincipalFactory,
                attributeRepository, attributeRepositoryAttributeMerger,
                servicesManager, attributeDefinitionStore,
                personDirectory);
        }

        @ConditionalOnMissingBean(name = "principalResolutionExecutionPlanConfigurer")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public PrincipalResolutionExecutionPlanConfigurer principalResolutionExecutionPlanConfigurer(
            @Qualifier("personDirectoryAttributeRepositoryPlan")
            final PersonDirectoryAttributeRepositoryPlan personDirectoryAttributeRepositoryPlan,
            @Qualifier("personDirectoryAttributeRepositoryPrincipalResolver")
            final PrincipalResolver personDirectoryAttributeRepositoryPrincipalResolver) {
            return plan -> {
                if (personDirectoryAttributeRepositoryPlan.isEmpty()) {
                    LOGGER.debug("Attribute repository sources are not available for person-directory principal resolution");
                } else {
                    LOGGER.trace("Attribute repository sources are defined and available for person-directory principal resolution chain. ");
                    plan.registerPrincipalResolver(personDirectoryAttributeRepositoryPrincipalResolver);
                }
            };
        }
    }

    @Configuration(value = "CasPersonDirectoryAttributeRepositoryPlanConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasPersonDirectoryAttributeRepositoryPlanConfiguration {
        @ConditionalOnMissingBean(name = "personDirectoryAttributeRepositoryPlan")
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public PersonDirectoryAttributeRepositoryPlan personDirectoryAttributeRepositoryPlan(
            final List<PersonDirectoryAttributeRepositoryPlanConfigurer> configurers,
            final ObjectProvider<List<PersonDirectoryAttributeRepositoryCustomizer>> customizers) {
            val plan = new DefaultPersonDirectoryAttributeRepositoryPlan(
                Optional.ofNullable(customizers.getIfAvailable()).orElseGet(ArrayList::new));
            configurers.forEach(c -> c.configureAttributeRepositoryPlan(plan));
            AnnotationAwareOrderComparator.sort(plan.getAttributeRepositories());
            LOGGER.trace("Final list of attribute repositories is [{}]", plan.getAttributeRepositories());
            return plan;
        }
    }

    @Configuration(value = "CasPersonDirectoryAttributeRepositoryConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasPersonDirectoryAttributeRepositoryConfiguration {
        private static AbstractAggregatingDefaultQueryPersonAttributeDao getAggregateAttributeRepository(
            final CasConfigurationProperties casProperties) {
            val properties = casProperties.getAuthn().getAttributeRepository();
            switch (properties.getCore().getAggregation()) {
                case CASCADE:
                    val dao = new CascadingPersonAttributeDao();
                    dao.setAddOriginalAttributesToQuery(true);
                    dao.setStopIfFirstDaoReturnsNull(true);
                    return dao;
                case MERGE:
                default:
                    return new MergingPersonAttributeDaoImpl();
            }
        }


        @Bean(name = {"cachingAttributeRepository", PrincipalResolver.BEAN_NAME_ATTRIBUTE_REPOSITORY})
        @ConditionalOnMissingBean(name = {"cachingAttributeRepository", PrincipalResolver.BEAN_NAME_ATTRIBUTE_REPOSITORY})
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public IPersonAttributeDao cachingAttributeRepository(
            final CasConfigurationProperties casProperties,
            @Qualifier("aggregatingAttributeRepository")
            final IPersonAttributeDao aggregatingAttributeRepository) {
            val props = casProperties.getAuthn().getAttributeRepository().getCore();
            if (props.getExpirationTime() <= 0) {
                LOGGER.warn("Attribute repository caching is disabled");
                return aggregatingAttributeRepository;
            }

            val impl = new CachingPersonAttributeDaoImpl();
            impl.setCacheNullResults(false);
            val userinfoCache = Caffeine.newBuilder()
                .maximumSize(props.getMaximumCacheSize())
                .expireAfterWrite(props.getExpirationTime(), TimeUnit.valueOf(props.getExpirationTimeUnit().toUpperCase(Locale.ENGLISH)))
                .build();
            impl.setUserInfoCache((Map) userinfoCache.asMap());
            impl.setCachedPersonAttributesDao(aggregatingAttributeRepository);
            LOGGER.trace("Configured cache expiration policy for attribute sources to be [{}] minute(s)", props.getExpirationTime());
            return impl;
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "attributeRepositoryAttributeMerger")
        public IAttributeMerger attributeRepositoryAttributeMerger(final CasConfigurationProperties casProperties) {
            return CoreAuthenticationUtils.getAttributeMerger(casProperties.getAuthn().getAttributeRepository().getCore().getMerger());
        }

        @Bean
        @ConditionalOnMissingBean(name = "aggregatingAttributeRepository")
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public IPersonAttributeDao aggregatingAttributeRepository(
            @Qualifier("attributeRepositoryAttributeMerger")
            final IAttributeMerger attributeRepositoryAttributeMerger,
            final CasConfigurationProperties casProperties,
            @Qualifier("personDirectoryAttributeRepositoryPlan")
            final PersonDirectoryAttributeRepositoryPlan personDirectoryAttributeRepositoryPlan) {
            val aggregate = getAggregateAttributeRepository(casProperties);
            aggregate.setMerger(attributeRepositoryAttributeMerger);

            val list = personDirectoryAttributeRepositoryPlan.getAttributeRepositories();
            aggregate.setPersonAttributeDaos(list);

            val properties = casProperties.getAuthn().getAttributeRepository();
            aggregate.setRequireAll(properties.getCore().isRequireAllRepositorySources());
            if (list.isEmpty()) {
                LOGGER.debug("No attribute repository sources are available/defined to merge together.");
            } else {
                val names = list
                    .stream()
                    .map(p -> Arrays.toString(p.getId()))
                    .collect(Collectors.joining(","));
                LOGGER.debug("Configured attribute repository sources to merge together: [{}]", names);
            }

            val recoverExceptions = properties.getCore().isRecoverExceptions();
            aggregate.setRecoverExceptions(recoverExceptions);
            LOGGER.trace("Configured attribute repository to recover from exceptions: [{}]", recoverExceptions);

            return aggregate;
        }

        @Bean
        @Lazy(false)
        public InitializingBean casPersonDirectoryInitializer(final CasConfigurationProperties casProperties) {
            return () -> FunctionUtils.doIf(LOGGER.isInfoEnabled(), value -> {
                val stub = casProperties.getAuthn().getAttributeRepository().getStub();
                val attrs = stub.getAttributes();
                if (!attrs.isEmpty()) {
                    LOGGER.info("Found and added static attributes [{}] to the list of candidate attribute repositories", attrs.keySet());
                }
            }).accept(null);
        }
    }

    @Configuration(value = "CasPersonDirectoryStaticSubAttributeRepositoryConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasPersonDirectoryStaticSubAttributeRepositoryConfiguration {

        @Configuration(value = "StubAttributeRepositoryConfiguration", proxyBeanMethods = false)
        @EnableConfigurationProperties(CasConfigurationProperties.class)
        public static class StubAttributeRepositoryConfiguration {
            @ConditionalOnMissingBean(name = "stubAttributeRepositories")
            @Bean
            @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
            public BeanContainer<IPersonAttributeDao> stubAttributeRepositories(final CasConfigurationProperties casProperties) {
                val list = new ArrayList<IPersonAttributeDao>();
                val stub = casProperties.getAuthn().getAttributeRepository().getStub();
                val attrs = stub.getAttributes();
                if (!attrs.isEmpty()) {
                    val dao = Beans.newStubAttributeRepository(casProperties.getAuthn().getAttributeRepository());
                    list.add(dao);
                }
                return BeanContainer.of(list);
            }
        }

        @Configuration(value = "StubAttributeRepositoryPlanConfiguration", proxyBeanMethods = false)
        @EnableConfigurationProperties(CasConfigurationProperties.class)
        public static class StubAttributeRepositoryPlanConfiguration {
            @Bean
            @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
            @ConditionalOnMissingBean(name = "stubPersonDirectoryAttributeRepositoryPlanConfigurer")
            public PersonDirectoryAttributeRepositoryPlanConfigurer stubPersonDirectoryAttributeRepositoryPlanConfigurer(
                @Qualifier("stubAttributeRepositories")
                final BeanContainer<IPersonAttributeDao> stubAttributeRepositories) {
                return plan -> {
                    val results = stubAttributeRepositories.toList()
                        .stream()
                        .filter(repo -> (Boolean) repo.getTags().get("state"))
                        .collect(Collectors.toList());
                    plan.registerAttributeRepositories(results);
                };
            }
        }
    }
}
