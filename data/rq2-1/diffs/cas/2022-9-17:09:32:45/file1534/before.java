package org.apereo.cas.config;

import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.CoreAuthenticationUtils;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.configuration.support.Beans;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.spnego.authentication.handler.support.JcifsConfig;
import org.apereo.cas.support.spnego.authentication.handler.support.JcifsSpnegoAuthenticationHandler;
import org.apereo.cas.support.spnego.authentication.handler.support.NtlmAuthenticationHandler;
import org.apereo.cas.support.spnego.authentication.principal.SpnegoPrincipalResolver;
import org.apereo.cas.util.ResourceUtils;
import org.apereo.cas.util.function.FunctionUtils;
import org.apereo.cas.util.spring.beans.BeanCondition;
import org.apereo.cas.util.spring.beans.BeanContainer;
import org.apereo.cas.util.spring.beans.BeanSupplier;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;

import jcifs.spnego.Authentication;
import lombok.val;
import org.apereo.services.persondir.IPersonAttributeDao;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;

import java.util.stream.Collectors;

/**
 * This is {@link SpnegoConfiguration}.
 *
 * @author Misagh Moayyed
 * @author Dmitriy Kopylenko
 * @since 5.0.0
 */
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.SPNEGO)
@AutoConfiguration
public class SpnegoConfiguration {

    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    @ConditionalOnMissingBean(name = "spnegoAuthentications")
    public BeanContainer<Authentication> spnegoAuthentications(
        final CasConfigurationProperties casProperties,
        final ConfigurableApplicationContext applicationContext) {
        val spnegoSystem = casProperties.getAuthn().getSpnego().getSystem();

        JcifsConfig.SystemSettings.initialize(applicationContext, spnegoSystem.getLoginConf());

        if (ResourceUtils.doesResourceExist(spnegoSystem.getKerberosConf())) {
            val kerbConf = applicationContext.getResource(spnegoSystem.getKerberosConf());
            FunctionUtils.doUnchecked(o -> JcifsConfig.SystemSettings.setKerberosConf(kerbConf.getFile().getCanonicalPath()));
        }

        JcifsConfig.SystemSettings.setKerberosDebug(spnegoSystem.getKerberosDebug());
        JcifsConfig.SystemSettings.setKerberosKdc(spnegoSystem.getKerberosKdc());
        JcifsConfig.SystemSettings.setKerberosRealm(spnegoSystem.getKerberosRealm());
        JcifsConfig.SystemSettings.setUseSubjectCredsOnly(spnegoSystem.isUseSubjectCredsOnly());

        val props = casProperties.getAuthn().getSpnego().getProperties();
        return BeanContainer.of(props.stream()
            .map(p -> {
                val c = new JcifsConfig();
                val jcifsSettings = c.getJcifsSettings();
                jcifsSettings.setJcifsDomain(p.getJcifsDomain());
                jcifsSettings.setJcifsDomainController(p.getJcifsDomainController());
                jcifsSettings.setJcifsNetbiosCachePolicy(p.getCachePolicy());
                jcifsSettings.setJcifsNetbiosWins(p.getJcifsNetbiosWins());
                jcifsSettings.setJcifsPassword(p.getJcifsPassword());
                jcifsSettings.setJcifsServicePassword(p.getJcifsServicePassword());
                jcifsSettings.setJcifsServicePrincipal(p.getJcifsServicePrincipal());
                jcifsSettings.setJcifsSocketTimeout(Beans.newDuration(p.getTimeout()).toMillis());
                jcifsSettings.setJcifsUsername(p.getJcifsUsername());
                return new Authentication(jcifsSettings.getProperties());
            })
            .collect(Collectors.toList()));
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "spnegoHandler")
    public AuthenticationHandler spnegoHandler(
        @Qualifier("spnegoAuthentications")
        final BeanContainer<Authentication> spnegoAuthentications,
        @Qualifier("spnegoPrincipalFactory")
        final PrincipalFactory spnegoPrincipalFactory,
        @Qualifier(ServicesManager.BEAN_NAME)
        final ServicesManager servicesManager,
        final CasConfigurationProperties casProperties) {
        val spnegoProperties = casProperties.getAuthn().getSpnego();
        return new JcifsSpnegoAuthenticationHandler(spnegoProperties.getName(),
            servicesManager, spnegoPrincipalFactory, spnegoAuthentications.toList(),
            spnegoProperties.isPrincipalWithDomainName(), spnegoProperties.isNtlmAllowed(),
            spnegoProperties.getOrder());
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public AuthenticationHandler ntlmAuthenticationHandler(
        final ConfigurableApplicationContext applicationContext,
        @Qualifier("ntlmPrincipalFactory")
        final PrincipalFactory ntlmPrincipalFactory,
        @Qualifier(ServicesManager.BEAN_NAME)
        final ServicesManager servicesManager,
        final CasConfigurationProperties casProperties) {
        return BeanSupplier.of(AuthenticationHandler.class)
            .when(BeanCondition.on("cas.authn.ntlm.enabled").isTrue().given(applicationContext.getEnvironment()))
            .supply(() -> {
                val ntlmProperties = casProperties.getAuthn().getNtlm();
                return new NtlmAuthenticationHandler(ntlmProperties.getName(),
                    servicesManager, ntlmPrincipalFactory,
                    ntlmProperties.isLoadBalance(), ntlmProperties.getDomainController(),
                    ntlmProperties.getIncludePattern(), ntlmProperties.getOrder());
            })
            .otherwiseProxy()
            .get();
    }

    @ConditionalOnMissingBean(name = "ntlmPrincipalFactory")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public PrincipalFactory ntlmPrincipalFactory() {
        return PrincipalFactoryUtils.newPrincipalFactory();
    }

    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "spnegoPrincipalResolver")
    public PrincipalResolver spnegoPrincipalResolver(
        @Qualifier("spnegoPrincipalFactory")
        final PrincipalFactory spnegoPrincipalFactory,
        @Qualifier(PrincipalResolver.BEAN_NAME_ATTRIBUTE_REPOSITORY)
        final IPersonAttributeDao attributeRepository,
        final CasConfigurationProperties casProperties) {
        val personDirectory = casProperties.getPersonDirectory();
        val spnegoPrincipal = casProperties.getAuthn().getSpnego().getPrincipal();
        val attributeMerger = CoreAuthenticationUtils.getAttributeMerger(
            casProperties.getAuthn().getAttributeRepository().getCore().getMerger());
        return CoreAuthenticationUtils.newPersonDirectoryPrincipalResolver(spnegoPrincipalFactory,
            attributeRepository, attributeMerger, SpnegoPrincipalResolver.class, spnegoPrincipal, personDirectory);
    }

    @ConditionalOnMissingBean(name = "spnegoPrincipalFactory")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public PrincipalFactory spnegoPrincipalFactory() {
        return PrincipalFactoryUtils.newPrincipalFactory();
    }

    @ConditionalOnMissingBean(name = "spnegoAuthenticationEventExecutionPlanConfigurer")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public AuthenticationEventExecutionPlanConfigurer spnegoAuthenticationEventExecutionPlanConfigurer(
        @Qualifier("ntlmAuthenticationHandler")
        final ObjectProvider<AuthenticationHandler> ntlmAuthenticationHandler,
        @Qualifier("spnegoPrincipalResolver")
        final PrincipalResolver spnegoPrincipalResolver,
        @Qualifier("spnegoHandler")
        final AuthenticationHandler spnegoHandler) {
        return plan -> {
            plan.registerAuthenticationHandlerWithPrincipalResolver(spnegoHandler, spnegoPrincipalResolver);
            ntlmAuthenticationHandler.ifAvailable(plan::registerAuthenticationHandler);
        };
    }
}
