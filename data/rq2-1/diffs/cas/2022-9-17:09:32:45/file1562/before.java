package org.apereo.cas.config;

import org.apereo.cas.adaptors.swivel.SwivelAuthenticationHandler;
import org.apereo.cas.adaptors.swivel.SwivelMultifactorAuthenticationProvider;
import org.apereo.cas.adaptors.swivel.SwivelTokenCredential;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.AuthenticationMetaDataPopulator;
import org.apereo.cas.authentication.MultifactorAuthenticationFailureModeEvaluator;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.authentication.bypass.MultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.handler.ByCredentialTypeAuthenticationHandlerResolver;
import org.apereo.cas.authentication.metadata.AuthenticationContextAttributeMetaDataPopulator;
import org.apereo.cas.authentication.metadata.MultifactorAuthenticationProviderMetadataPopulator;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link SwivelAuthenticationEventExecutionPlanConfiguration}.
 *
 * @author Misagh Moayyed
 * @author Dmitriy Kopylenko
 * @since 5.2.0
 * @deprecated Since 6.6.
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.MultifactorAuthentication, module = "swivel")
@AutoConfiguration
@Deprecated(since = "6.6")
public class SwivelAuthenticationEventExecutionPlanConfiguration {

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "swivelMultifactorProviderAuthenticationMetadataPopulator")
    public AuthenticationMetaDataPopulator swivelMultifactorProviderAuthenticationMetadataPopulator(
        @Qualifier(ServicesManager.BEAN_NAME)
        final ServicesManager servicesManager,
        final CasConfigurationProperties casProperties,
        @Qualifier("swivelMultifactorAuthenticationProvider")
        final ObjectProvider<MultifactorAuthenticationProvider> multifactorAuthenticationProvider) {
        val authenticationContextAttribute = casProperties.getAuthn().getMfa().getCore().getAuthenticationContextAttribute();
        return new MultifactorAuthenticationProviderMetadataPopulator(authenticationContextAttribute,
            multifactorAuthenticationProvider, servicesManager);
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "swivelAuthenticationMetaDataPopulator")
    public AuthenticationMetaDataPopulator swivelAuthenticationMetaDataPopulator(
        final CasConfigurationProperties casProperties,
        @Qualifier("swivelAuthenticationHandler")
        final AuthenticationHandler swivelAuthenticationHandler,
        @Qualifier("swivelMultifactorAuthenticationProvider")
        final MultifactorAuthenticationProvider swivelMultifactorAuthenticationProvider) {
        val authenticationContextAttribute = casProperties.getAuthn().getMfa().getCore().getAuthenticationContextAttribute();
        return new AuthenticationContextAttributeMetaDataPopulator(authenticationContextAttribute,
            swivelAuthenticationHandler, swivelMultifactorAuthenticationProvider.getId());
    }

    @ConditionalOnMissingBean(name = "swivelPrincipalFactory")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public PrincipalFactory swivelPrincipalFactory() {
        return PrincipalFactoryUtils.newPrincipalFactory();
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "swivelAuthenticationHandler")
    public AuthenticationHandler swivelAuthenticationHandler(
        @Qualifier("swivelMultifactorAuthenticationProvider")
        final ObjectProvider<MultifactorAuthenticationProvider> multifactorAuthenticationProvider,
        final CasConfigurationProperties casProperties,
        @Qualifier("swivelPrincipalFactory")
        final PrincipalFactory swivelPrincipalFactory,
        @Qualifier(ServicesManager.BEAN_NAME)
        final ServicesManager servicesManager) {
        val swivel = casProperties.getAuthn().getMfa().getSwivel();
        return new SwivelAuthenticationHandler(swivel.getName(),
            servicesManager, swivelPrincipalFactory, swivel, multifactorAuthenticationProvider);
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "swivelMultifactorAuthenticationProvider")
    public MultifactorAuthenticationProvider swivelMultifactorAuthenticationProvider(
        final CasConfigurationProperties casProperties,
        @Qualifier("swivelBypassEvaluator")
        final MultifactorAuthenticationProviderBypassEvaluator swivelBypassEvaluator,
        @Qualifier("failureModeEvaluator")
        final MultifactorAuthenticationFailureModeEvaluator failureModeEvaluator) {
        val swivel = casProperties.getAuthn().getMfa().getSwivel();
        val p = new SwivelMultifactorAuthenticationProvider(swivel.getSwivelUrl());
        p.setBypassEvaluator(swivelBypassEvaluator);
        p.setFailureMode(swivel.getFailureMode());
        p.setFailureModeEvaluator(failureModeEvaluator);
        p.setOrder(swivel.getRank());
        p.setId(swivel.getId());
        return p;
    }

    @ConditionalOnMissingBean(name = "swivelAuthenticationEventExecutionPlanConfigurer")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public AuthenticationEventExecutionPlanConfigurer swivelAuthenticationEventExecutionPlanConfigurer(
        @Qualifier("swivelMultifactorProviderAuthenticationMetadataPopulator")
        final AuthenticationMetaDataPopulator swivelMultifactorProviderAuthenticationMetadataPopulator,
        @Qualifier("swivelAuthenticationHandler")
        final AuthenticationHandler swivelAuthenticationHandler,
        @Qualifier("swivelAuthenticationMetaDataPopulator")
        final AuthenticationMetaDataPopulator swivelAuthenticationMetaDataPopulator) {
        return plan -> {
            plan.registerAuthenticationHandler(swivelAuthenticationHandler);
            plan.registerAuthenticationMetadataPopulator(swivelAuthenticationMetaDataPopulator);
            plan.registerAuthenticationMetadataPopulator(swivelMultifactorProviderAuthenticationMetadataPopulator);
            plan.registerAuthenticationHandlerResolver(new ByCredentialTypeAuthenticationHandlerResolver(SwivelTokenCredential.class));
        };
    }
}
