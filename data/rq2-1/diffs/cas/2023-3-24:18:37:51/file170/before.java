package org.apereo.cas.config;

import org.apereo.cas.adaptors.yubikey.YubiKeyAccountRegistry;
import org.apereo.cas.adaptors.yubikey.YubiKeyAccountValidator;
import org.apereo.cas.adaptors.yubikey.dao.CouchDbYubiKeyAccountRegistry;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.couchdb.core.CouchDbConnectorFactory;
import org.apereo.cas.couchdb.core.DefaultCouchDbConnectorFactory;
import org.apereo.cas.couchdb.yubikey.YubiKeyAccountCouchDbRepository;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import lombok.val;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.impl.ObjectMapperFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link CouchDbYubiKeyConfiguration}.
 *
 * @author Timur Duehr
 * @since 6.0.0
 * @deprecated Since 7
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.YubiKey, module = "couchdb")
@AutoConfiguration
@Deprecated(since = "7.0.0")
public class CouchDbYubiKeyConfiguration {

    @ConditionalOnMissingBean(name = "couchDbYubiKeyAccountRepository")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public YubiKeyAccountCouchDbRepository couchDbYubiKeyAccountRepository(
        final CasConfigurationProperties casProperties,
        @Qualifier("yubikeyCouchDbFactory")
        final CouchDbConnectorFactory yubikeyCouchDbFactory) {
        val couchDb = casProperties.getAuthn().getMfa().getYubikey().getCouchDb();
        return new YubiKeyAccountCouchDbRepository(yubikeyCouchDbFactory.getCouchDbConnector(), couchDb.isCreateIfNotExists());
    }

    @ConditionalOnMissingBean(name = "yubikeyCouchDbInstance")
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    public CouchDbInstance yubikeyCouchDbInstance(
        @Qualifier("yubikeyCouchDbFactory")
        final CouchDbConnectorFactory yubikeyCouchDbFactory) {
        return yubikeyCouchDbFactory.getCouchDbInstance();
    }

    @ConditionalOnMissingBean(name = "yubikeyCouchDbConnector")
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    public CouchDbConnector yubikeyCouchDbConnector(
        @Qualifier("yubikeyCouchDbFactory")
        final CouchDbConnectorFactory yubikeyCouchDbFactory) {
        return yubikeyCouchDbFactory.getCouchDbConnector();
    }

    @ConditionalOnMissingBean(name = "yubikeyCouchDbFactory")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public CouchDbConnectorFactory yubikeyCouchDbFactory(final CasConfigurationProperties casProperties,
                                                         @Qualifier("defaultObjectMapperFactory")
                                                         final ObjectMapperFactory objectMapperFactory) {
        return new DefaultCouchDbConnectorFactory(casProperties.getAuthn().getMfa().getYubikey().getCouchDb(), objectMapperFactory);
    }

    @ConditionalOnMissingBean(name = "couchDbYubikeyAccountRegistry")
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    public YubiKeyAccountRegistry yubiKeyAccountRegistry(
        @Qualifier("couchDbYubiKeyAccountRepository")
        final YubiKeyAccountCouchDbRepository couchDbYubiKeyAccountRepository,
        @Qualifier("yubiKeyAccountValidator")
        final YubiKeyAccountValidator yubiKeyAccountValidator,
        @Qualifier("yubikeyAccountCipherExecutor")
        final CipherExecutor yubikeyAccountCipherExecutor) {
        val registry = new CouchDbYubiKeyAccountRegistry(yubiKeyAccountValidator, couchDbYubiKeyAccountRepository);
        registry.setCipherExecutor(yubikeyAccountCipherExecutor);
        return registry;
    }
}
