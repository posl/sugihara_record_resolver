package org.apereo.cas.config;

import org.apereo.cas.adaptors.u2f.storage.U2FCouchDbDeviceRepository;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.couchdb.core.CouchDbConnectorFactory;
import org.apereo.cas.couchdb.core.DefaultCouchDbConnectorFactory;
import org.apereo.cas.couchdb.u2f.U2FDeviceRegistrationCouchDbRepository;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.ektorp.impl.ObjectMapperFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link U2FCouchDbConfiguration}.
 *
 * @author Timur Duehr
 * @since 6.0.0
 * @deprecated Since 7
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.U2F, module = "couchdb")
@AutoConfiguration
@Deprecated(since = "7.0.0")
public class U2FCouchDbConfiguration {

    @ConditionalOnMissingBean(name = "u2fCouchDbFactory")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public CouchDbConnectorFactory u2fCouchDbFactory(
        final CasConfigurationProperties casProperties,
        @Qualifier("defaultObjectMapperFactory")
        final ObjectMapperFactory objectMapperFactory) {
        return new DefaultCouchDbConnectorFactory(casProperties.getAuthn().getMfa().getU2f().getCouchDb(), objectMapperFactory);
    }

    @ConditionalOnMissingBean(name = "couchDbU2fDeviceRegistrationRepository")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public U2FDeviceRegistrationCouchDbRepository couchDbU2fDeviceRegistrationRepository(
        @Qualifier("u2fCouchDbFactory")
        final CouchDbConnectorFactory u2fCouchDbFactory,
        final CasConfigurationProperties casProperties) {
        val couchDb = casProperties.getAuthn().getMfa().getU2f().getCouchDb();
        return new U2FDeviceRegistrationCouchDbRepository(u2fCouchDbFactory.getCouchDbConnector(),
            u2fCouchDbFactory.getCouchDbInstance(), couchDb.isCreateIfNotExists());
    }

    @ConditionalOnMissingBean(name = "couchDbU2fDeviceRepository")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public U2FCouchDbDeviceRepository u2fDeviceRepository(
        @Qualifier("u2fRegistrationRecordCipherExecutor")
        final CipherExecutor u2fRegistrationRecordCipherExecutor,
        @Qualifier("couchDbU2fDeviceRegistrationRepository")
        final U2FDeviceRegistrationCouchDbRepository couchDbU2fDeviceRegistrationRepository,
        final CasConfigurationProperties casProperties) {
        val u2f = casProperties.getAuthn().getMfa().getU2f();
        final LoadingCache<String, String> requestStorage =
            Caffeine.newBuilder().expireAfterWrite(u2f.getCore().getExpireRegistrations(),
                u2f.getCore().getExpireRegistrationsTimeUnit()).build(key -> StringUtils.EMPTY);
        return new U2FCouchDbDeviceRepository(requestStorage,
            couchDbU2fDeviceRegistrationRepository, casProperties, u2fRegistrationRecordCipherExecutor);
    }
}
