package org.apereo.cas.config;

import org.apereo.cas.audit.AuditTrailExecutionPlanConfigurer;
import org.apereo.cas.audit.CouchDbAuditTrailManager;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.couchdb.audit.AuditActionContextCouchDbRepository;
import org.apereo.cas.couchdb.core.CouchDbConnectorFactory;
import org.apereo.cas.couchdb.core.DefaultCouchDbConnectorFactory;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import org.apereo.inspektr.audit.AuditTrailManager;
import org.ektorp.impl.ObjectMapperFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link CasSupportCouchDbAuditConfiguration}.
 *
 * @author Timur Duehr
 * @since 6.0.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.Audit, module = "couchdb")
@AutoConfiguration
public class CasSupportCouchDbAuditConfiguration {

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "auditCouchDbFactory")
    public CouchDbConnectorFactory auditCouchDbFactory(
        final CasConfigurationProperties casProperties,
        @Qualifier("defaultObjectMapperFactory")
        final ObjectMapperFactory defaultObjectMapperFactory) {
        return new DefaultCouchDbConnectorFactory(casProperties.getAudit().getCouchDb(), defaultObjectMapperFactory);
    }

    @ConditionalOnMissingBean(name = "auditActionContextCouchDbRepository")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public AuditActionContextCouchDbRepository auditActionContextCouchDbRepository(
        @Qualifier("auditCouchDbFactory")
        final CouchDbConnectorFactory auditCouchDbFactory, final CasConfigurationProperties casProperties) {
        return new AuditActionContextCouchDbRepository(auditCouchDbFactory.getCouchDbConnector(), casProperties.getAudit().getCouchDb().isCreateIfNotExists());
    }

    @ConditionalOnMissingBean(name = "couchDbAuditTrailManager")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public AuditTrailManager couchDbAuditTrailManager(
        @Qualifier("auditActionContextCouchDbRepository")
        final AuditActionContextCouchDbRepository repository, final CasConfigurationProperties casProperties) {
        return new CouchDbAuditTrailManager(casProperties.getAudit().getCouchDb().isAsynchronous(), repository);
    }

    @ConditionalOnMissingBean(name = "couchDbAuditTrailExecutionPlanConfigurer")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public AuditTrailExecutionPlanConfigurer couchDbAuditTrailExecutionPlanConfigurer(
        @Qualifier("couchDbAuditTrailManager")
        final AuditTrailManager auditTrailManager) {
        return plan -> plan.registerAuditTrailManager(auditTrailManager);
    }
}
