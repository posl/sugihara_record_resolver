package org.apereo.cas.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.couchdb.audit.AuditActionContextCouchDbRepository;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;
import org.apereo.cas.web.support.CouchDbThrottledSubmissionHandlerInterceptorAdapter;
import org.apereo.cas.web.support.ThrottledSubmissionHandlerConfigurationContext;
import org.apereo.cas.web.support.ThrottledSubmissionHandlerInterceptor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link CasCouchDbThrottlingConfiguration}.
 *
 * @author Timur Duehr
 * @since 6.0.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.Throttling, module = "couchdb")
@AutoConfiguration
public class CasCouchDbThrottlingConfiguration {

    @ConditionalOnMissingBean(name = "couchDbAuthenticationThrottle")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public ThrottledSubmissionHandlerInterceptor authenticationThrottle(
        @Qualifier("auditActionContextCouchDbRepository")
        final AuditActionContextCouchDbRepository couchDbRepository,
        @Qualifier("authenticationThrottlingConfigurationContext")
        final ThrottledSubmissionHandlerConfigurationContext authenticationThrottlingConfigurationContext) {
        return new CouchDbThrottledSubmissionHandlerInterceptorAdapter(authenticationThrottlingConfigurationContext, couchDbRepository);
    }
}
