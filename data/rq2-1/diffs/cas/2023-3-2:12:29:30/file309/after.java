package org.apereo.cas.config;

import org.apereo.cas.CasProtocolConstants;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.authentication.principal.ServiceFactoryConfigurer;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.configuration.support.Beans;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;
import org.apereo.cas.web.CasYamlHttpMessageConverter;
import org.apereo.cas.web.ProtocolEndpointWebSecurityConfigurer;
import org.apereo.cas.web.SimpleUrlValidatorFactoryBean;
import org.apereo.cas.web.UrlValidator;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.support.ArgumentExtractor;
import org.apereo.cas.web.support.DefaultArgumentExtractor;
import org.apereo.cas.web.view.CasReloadableMessageBundle;
import org.apereo.cas.web.view.DynamicHtmlView;

import lombok.val;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * This is {@link CasCoreWebConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.Core)
@AutoConfiguration
public class CasCoreWebConfiguration {

    @Configuration(value = "CasCoreWebMessageSourceConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreWebMessageSourceConfiguration {
        /**
         * Load property files containing non-i18n fallback values
         * that should be exposed to Thyme templates.
         * keys in properties files added last will take precedence over the
         * internal cas_common_messages.properties.
         * Keys in regular messages bundles will override any of the common messages.
         *
         * @param casProperties the cas properties
         * @return PropertiesFactoryBean containing all common (non-i18n) messages
         */
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public PropertiesFactoryBean casCommonMessages(final CasConfigurationProperties casProperties) {
            val properties = new PropertiesFactoryBean();
            val resourceLoader = new DefaultResourceLoader();
            val commonNames = casProperties.getMessageBundle().getCommonNames();

            val resourceList = commonNames
                .stream()
                .map(resourceLoader::getResource)
                .collect(Collectors.toList());
            resourceList.add(resourceLoader.getResource("classpath:/cas_common_messages.properties"));
            properties.setLocations(resourceList.toArray(Resource[]::new));
            properties.setSingleton(true);
            properties.setIgnoreResourceNotFound(true);
            return properties;
        }

        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @Bean
        public HierarchicalMessageSource messageSource(
            final CasConfigurationProperties casProperties,
            @Qualifier("casCommonMessages")
            final Properties casCommonMessages) {
            val bean = new CasReloadableMessageBundle();
            val mb = casProperties.getMessageBundle();
            bean.setDefaultEncoding(mb.getEncoding());
            bean.setCacheSeconds(Long.valueOf(Beans.newDuration(mb.getCacheSeconds()).toSeconds()).intValue());
            bean.setFallbackToSystemLocale(mb.isFallbackSystemLocale());
            bean.setUseCodeAsDefaultMessage(mb.isUseCodeMessage());
            bean.setBasenames(mb.getBaseNames().toArray(ArrayUtils.EMPTY_STRING_ARRAY));
            bean.setCommonMessages(casCommonMessages);
            return bean;
        }
    }

    @Configuration(value = "CasCoreWebRequestsConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreWebRequestsConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = ArgumentExtractor.BEAN_NAME)
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public ArgumentExtractor argumentExtractor(final List<ServiceFactoryConfigurer> configurers) {
            val serviceFactoryList = new ArrayList<ServiceFactory<? extends WebApplicationService>>();
            configurers.forEach(c -> serviceFactoryList.addAll(c.buildServiceFactories()));
            AnnotationAwareOrderComparator.sortIfNecessary(configurers);
            return new DefaultArgumentExtractor(serviceFactoryList);
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = UrlValidator.BEAN_NAME)
        public FactoryBean<UrlValidator> urlValidator(final CasConfigurationProperties casProperties) {
            val httpClient = casProperties.getHttpClient();
            val allowLocalLogoutUrls = httpClient.isAllowLocalUrls();
            val authorityValidationRegEx = httpClient.getAuthorityValidationRegex();
            val authorityValidationRegExCaseSensitive = httpClient.isAuthorityValidationRegExCaseSensitive();
            return new SimpleUrlValidatorFactoryBean(allowLocalLogoutUrls, authorityValidationRegEx,
                authorityValidationRegExCaseSensitive);
        }

        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "yamlHttpMessageConverter")
        public HttpMessageConverter yamlHttpMessageConverter() {
            return new CasYamlHttpMessageConverter();
        }
    }

    @Configuration(value = "CasCoreWebViewsConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreWebViewsConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = CasWebflowConstants.VIEW_ID_DYNAMIC_HTML)
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        public View dynamicHtmlView() {
            return (model, request, response) -> {
                val html = (String) Objects.requireNonNull(model).get(DynamicHtmlView.class.getName());
                new DynamicHtmlView(html).render(model, request, response);
            };
        }
    }

    @Configuration(value = "CasCoreWebEndpointsConfiguration", proxyBeanMethods = false)
    @EnableConfigurationProperties(CasConfigurationProperties.class)
    public static class CasCoreWebEndpointsConfiguration {
        @Bean
        @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
        @ConditionalOnMissingBean(name = "casProtocolEndpointConfigurer")
        public ProtocolEndpointWebSecurityConfigurer<Void> casProtocolEndpointConfigurer() {
            return new ProtocolEndpointWebSecurityConfigurer<>() {
                @Override
                public List<String> getIgnoredEndpoints() {
                    return List.of(
                        StringUtils.prependIfMissing(CasProtocolConstants.ENDPOINT_LOGIN, "/"),
                        StringUtils.prependIfMissing(CasProtocolConstants.ENDPOINT_LOGOUT, "/"),
                        StringUtils.prependIfMissing(CasProtocolConstants.ENDPOINT_VALIDATE, "/"),
                        StringUtils.prependIfMissing(CasProtocolConstants.ENDPOINT_SERVICE_VALIDATE, "/"),
                        StringUtils.prependIfMissing(CasProtocolConstants.ENDPOINT_SERVICE_VALIDATE_V3, "/"),
                        StringUtils.prependIfMissing(CasProtocolConstants.ENDPOINT_PROXY_VALIDATE, "/"),
                        StringUtils.prependIfMissing(CasProtocolConstants.ENDPOINT_PROXY_VALIDATE_V3, "/"),
                        StringUtils.prependIfMissing(CasProtocolConstants.ENDPOINT_PROXY, "/"));
                }
            };
        }
    }
}
