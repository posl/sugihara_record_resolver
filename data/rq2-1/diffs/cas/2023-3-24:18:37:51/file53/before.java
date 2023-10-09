package org.apereo.cas.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.consent.ConsentRepository;
import org.apereo.cas.consent.CouchDbConsentRepository;
import org.apereo.cas.couchdb.consent.ConsentDecisionCouchDbRepository;
import org.apereo.cas.couchdb.core.CouchDbConnectorFactory;
import org.apereo.cas.couchdb.core.DefaultCouchDbConnectorFactory;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import lombok.val;
import org.ektorp.impl.ObjectMapperFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link CasConsentCouchDbConfiguration}.
 *
 * @author Timur Duehr
 * @since 6.0.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.Consent, module = "couchdb")
@AutoConfiguration
public class CasConsentCouchDbConfiguration {

    @ConditionalOnMissingBean(name = "consentCouchDbFactory")
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    public CouchDbConnectorFactory consentCouchDbFactory(
        final CasConfigurationProperties casProperties,
        @Qualifier("defaultObjectMapperFactory")
        final ObjectMapperFactory objectMapperFactory) {
        return new DefaultCouchDbConnectorFactory(casProperties.getConsent().getCouchDb(), objectMapperFactory);
    }

    @ConditionalOnMissingBean(name = "consentCouchDbRepository")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public ConsentDecisionCouchDbRepository consentCouchDbRepository(
        @Qualifier("consentCouchDbFactory")
        final CouchDbConnectorFactory consentCouchDbFactory,
        final CasConfigurationProperties casProperties) {
        val repository = new ConsentDecisionCouchDbRepository(consentCouchDbFactory.getCouchDbConnector(),
            consentCouchDbFactory.getCouchDbInstance(),
            casProperties.getConsent().getCouchDb().isCreateIfNotExists());
        repository.initStandardDesignDocument();
        return repository;
    }

    @ConditionalOnMissingBean(name = "couchDbConsentRepository")
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    public ConsentRepository consentRepository(
        @Qualifier("consentCouchDbRepository")
        final ConsentDecisionCouchDbRepository consentCouchDbRepository) {
        return new CouchDbConsentRepository(consentCouchDbRepository);
    }
}
