package org.apereo.cas.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.couchdb.core.CouchDbConnectorFactory;
import org.apereo.cas.couchdb.saml.SamlMetadataDocumentCouchDbRepository;
import org.apereo.cas.support.saml.OpenSamlConfigBean;
import org.apereo.cas.support.saml.metadata.resolver.CouchDbSamlRegisteredServiceMetadataResolver;
import org.apereo.cas.support.saml.services.idp.metadata.cache.resolver.SamlRegisteredServiceMetadataResolver;
import org.apereo.cas.support.saml.services.idp.metadata.plan.SamlRegisteredServiceMetadataResolutionPlanConfigurer;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link SamlIdPCouchDbRegisteredServiceMetadataConfiguration}.
 *
 * @author Timur Duehr
 * @since 6.0.0
 * @deprecated Since 7
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.SAMLServiceProviderMetadata, module = "couchdb")
@AutoConfiguration
@Deprecated(since = "7.0.0")
public class SamlIdPCouchDbRegisteredServiceMetadataConfiguration {

    @ConditionalOnMissingBean(name = "samlMetadataDocumentCouchDbRepository")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public SamlMetadataDocumentCouchDbRepository samlMetadataDocumentCouchDbRepository(
        final CasConfigurationProperties casProperties,
        @Qualifier("samlMetadataCouchDbFactory")
        final CouchDbConnectorFactory samlMetadataCouchDbFactory) {
        val couch = casProperties.getAuthn()
            .getSamlIdp()
            .getMetadata()
            .getCouchDb();
        return new SamlMetadataDocumentCouchDbRepository(samlMetadataCouchDbFactory.getCouchDbConnector(), couch.isCreateIfNotExists());
    }

    @ConditionalOnMissingBean(name = "couchDbSamlRegisteredServiceMetadataResolver")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public SamlRegisteredServiceMetadataResolver couchDbSamlRegisteredServiceMetadataResolver(
        final CasConfigurationProperties casProperties,
        @Qualifier("samlMetadataDocumentCouchDbRepository")
        final SamlMetadataDocumentCouchDbRepository samlMetadataDocumentCouchDbRepository,
        @Qualifier(OpenSamlConfigBean.DEFAULT_BEAN_NAME)
        final OpenSamlConfigBean openSamlConfigBean) {
        val idp = casProperties.getAuthn().getSamlIdp();
        return new CouchDbSamlRegisteredServiceMetadataResolver(idp, openSamlConfigBean, samlMetadataDocumentCouchDbRepository);
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "couchDbSamlRegisteredServiceMetadataResolutionPlanConfigurer")
    public SamlRegisteredServiceMetadataResolutionPlanConfigurer couchDbSamlRegisteredServiceMetadataResolutionPlanConfigurer(
        @Qualifier("couchDbSamlRegisteredServiceMetadataResolver")
        final SamlRegisteredServiceMetadataResolver couchDbSamlRegisteredServiceMetadataResolver) {
        return plan -> plan.registerMetadataResolver(couchDbSamlRegisteredServiceMetadataResolver);
    }
}
