package org.apereo.cas.config;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.couchdb.core.CouchDbConnectorFactory;
import org.apereo.cas.couchdb.core.DefaultCouchDbConnectorFactory;
import org.apereo.cas.couchdb.gauth.credential.GoogleAuthenticatorAccountCouchDbRepository;
import org.apereo.cas.couchdb.gauth.token.GoogleAuthenticatorTokenCouchDbRepository;
import org.apereo.cas.gauth.credential.CouchDbGoogleAuthenticatorTokenCredentialRepository;
import org.apereo.cas.gauth.token.GoogleAuthenticatorCouchDbTokenRepository;
import org.apereo.cas.otp.repository.credentials.OneTimeTokenCredentialRepository;
import org.apereo.cas.otp.repository.token.OneTimeTokenRepository;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import com.warrenstrange.googleauth.IGoogleAuthenticator;
import lombok.val;
import org.ektorp.impl.ObjectMapperFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * This is {@link GoogleAuthenticatorCouchDbConfiguration}.
 *
 * @author Timur Duehr
 * @since 6.0.0
 * @deprecated Since 7
 */
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.GoogleAuthenticator, module = "couchdb")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@EnableScheduling
@AutoConfiguration
@Deprecated(since = "7.0.0")
public class GoogleAuthenticatorCouchDbConfiguration {

    @ConditionalOnMissingBean(name = "oneTimeTokenAccountCouchDbFactory")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public CouchDbConnectorFactory oneTimeTokenAccountCouchDbFactory(final CasConfigurationProperties casProperties,
                                                                     @Qualifier("defaultObjectMapperFactory")
                                                                     final ObjectMapperFactory objectMapperFactory) {
        return new DefaultCouchDbConnectorFactory(casProperties.getAuthn().getMfa().getGauth().getCouchDb(), objectMapperFactory);
    }

    @ConditionalOnMissingBean(name = "couchDbGoogleAuthenticatorAccountRegistry")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public OneTimeTokenCredentialRepository googleAuthenticatorAccountRegistry(
        @Qualifier("googleAuthenticatorInstance")
        final IGoogleAuthenticator googleAuthenticatorInstance,
        @Qualifier("googleAuthenticatorAccountCipherExecutor")
        final CipherExecutor googleAuthenticatorAccountCipherExecutor,
        @Qualifier("googleAuthenticatorScratchCodesCipherExecutor")
        final CipherExecutor googleAuthenticatorScratchCodesCipherExecutor,
        @Qualifier("couchDbOneTimeTokenAccountRepository")
        final GoogleAuthenticatorAccountCouchDbRepository couchDbRepository) {
        return new CouchDbGoogleAuthenticatorTokenCredentialRepository(googleAuthenticatorInstance, couchDbRepository, googleAuthenticatorAccountCipherExecutor,
                googleAuthenticatorScratchCodesCipherExecutor);
    }

    @ConditionalOnMissingBean(name = "couchDbOneTimeTokenAccountRepository")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public GoogleAuthenticatorAccountCouchDbRepository couchDbOneTimeTokenAccountRepository(
        @Qualifier("oneTimeTokenAccountCouchDbFactory")
        final CouchDbConnectorFactory oneTimeTokenCouchDbFactory, final CasConfigurationProperties casProperties) {
        val repository = new GoogleAuthenticatorAccountCouchDbRepository(oneTimeTokenCouchDbFactory.getCouchDbConnector(),
            casProperties.getAuthn().getMfa().getGauth().getCouchDb().isCreateIfNotExists());
        repository.initStandardDesignDocument();
        return repository;
    }

    @ConditionalOnMissingBean(name = "couchDbOneTimeTokenAutneticatorTokenRepository")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public OneTimeTokenRepository oneTimeTokenAuthenticatorTokenRepository(
        @Qualifier("couchDbOneTimeTokenRepository")
        final GoogleAuthenticatorTokenCouchDbRepository couchDbOneTimeTokenRepository, final CasConfigurationProperties casProperties) {
        return new GoogleAuthenticatorCouchDbTokenRepository(couchDbOneTimeTokenRepository, casProperties.getAuthn().getMfa().getGauth().getCore().getTimeStepSize());
    }

    @ConditionalOnMissingBean(name = "oneTimeTokenCouchDbFactory")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public CouchDbConnectorFactory oneTimeTokenCouchDbFactory(final CasConfigurationProperties casProperties,
                                                              @Qualifier("defaultObjectMapperFactory")
                                                              final ObjectMapperFactory objectMapperFactory) {
        return new DefaultCouchDbConnectorFactory(casProperties.getAuthn().getMfa().getGauth().getCouchDb(), objectMapperFactory);
    }

    @ConditionalOnMissingBean(name = "couchDbbOneTimeTokenRepository")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public GoogleAuthenticatorTokenCouchDbRepository couchDbOneTimeTokenRepository(
        @Qualifier("oneTimeTokenAccountCouchDbFactory")
        final CouchDbConnectorFactory oneTimeTokenCouchDbFactory, final CasConfigurationProperties casProperties) {
        return new GoogleAuthenticatorTokenCouchDbRepository(oneTimeTokenCouchDbFactory.getCouchDbConnector(),
            casProperties.getAuthn().getMfa().getGauth().getCouchDb().isCreateIfNotExists());
    }
}
