package org.apereo.cas.config;

import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationException;
import org.apereo.cas.authentication.AuthenticationHandlerResolver;
import org.apereo.cas.authentication.AuthenticationManager;
import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.authentication.DefaultAuthenticationTransactionFactory;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.authentication.principal.RegisteredServicePrincipalAttributesRepository;
import org.apereo.cas.config.support.CasWebApplicationServiceFactoryConfiguration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.monitor.config.CasCoreMonitorConfiguration;
import org.apereo.cas.validation.Assertion;
import org.apereo.cas.validation.AuthenticationAttributeReleasePolicy;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link CasCoreAuthenticationSupportConfigurationTests}.
 *
 * @author Misagh Moayyed
 * @since 6.3.0
 */
@SpringBootTest(classes = {
    CasCoreAuthenticationSupportConfigurationTests.CasCoreAuthenticationSupportTestConfiguration.class,
    
    RefreshAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    ObservationAutoConfiguration.class,
    CasPersonDirectoryTestConfiguration.class,
    CasCoreServicesConfiguration.class,
    CasCoreUtilConfiguration.class,
    CasCoreNotificationsConfiguration.class,
    CasCoreWebConfiguration.class,
    CasCoreHttpConfiguration.class,
    CasCoreMonitorConfiguration.class,
    CasCoreAuthenticationMonitoringConfiguration.class,
    CasWebApplicationServiceFactoryConfiguration.class,
    CasCoreAuthenticationPrincipalConfiguration.class,
    CasCoreAuthenticationConfiguration.class,
    CasCoreAuthenticationHandlersConfiguration.class,
    CasCoreAuthenticationPolicyConfiguration.class,
    CasCoreAuthenticationSupportConfiguration.class
},
    properties = {
        "cas.authn.core.groovy-authentication-resolution.location=classpath:GroovyAuthenticationHandlerResolver.groovy",
        "cas.authn.core.engine.groovy-pre-processor.location=classpath:GroovyPreProcessor.groovy",
        "cas.authn.core.engine.groovy-post-processor.location=classpath:GroovyPostProcessor.groovy",
        "cas.authn.authentication-attribute-release.enabled=false",
        "cas.authn.attribute-repository.core.expiration-time=0",
        "cas.authn.policy.source-selection-enabled=true"
    })
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Tag("Authentication")
public class CasCoreAuthenticationSupportConfigurationTests {
    @Autowired
    @Qualifier(AuthenticationManager.BEAN_NAME)
    private AuthenticationManager authenticationManager;

    @Autowired
    @Qualifier(PrincipalResolver.BEAN_NAME_GLOBAL_PRINCIPAL_ATTRIBUTE_REPOSITORY)
    private RegisteredServicePrincipalAttributesRepository globalPrincipalAttributeRepository;

    @Autowired
    @Qualifier(AuthenticationAttributeReleasePolicy.BEAN_NAME)
    private AuthenticationAttributeReleasePolicy authenticationAttributeReleasePolicy;

    @Autowired
    @Qualifier("groovyAuthenticationHandlerResolver")
    private AuthenticationHandlerResolver groovyAuthenticationHandlerResolver;

    @Autowired
    @Qualifier("groovyAuthenticationProcessorExecutionPlanConfigurer")
    private AuthenticationEventExecutionPlanConfigurer groovyAuthenticationProcessorExecutionPlanConfigurer;

    @Test
    public void verifyOperation() {
        assertNotNull(groovyAuthenticationHandlerResolver);
        assertNotNull(globalPrincipalAttributeRepository);
        assertNotNull(authenticationManager);
        assertNotNull(groovyAuthenticationProcessorExecutionPlanConfigurer);

        val attributes = authenticationAttributeReleasePolicy.getAuthenticationAttributesForRelease(
            CoreAuthenticationTestUtils.getAuthentication(), mock(Assertion.class),
            Map.of(), CoreAuthenticationTestUtils.getRegisteredService());
        assertTrue(attributes.isEmpty());

        assertThrows(AuthenticationException.class, () -> authenticationManager.authenticate(new DefaultAuthenticationTransactionFactory()
            .newTransaction(CoreAuthenticationTestUtils.getCredentialsWithSameUsernameAndPassword())));
    }

    @TestConfiguration(value = "CasCoreAuthenticationSupportConfigurationTestConfiguration", proxyBeanMethods = false)
    public static class CasCoreAuthenticationSupportTestConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = AuthenticationServiceSelectionPlan.BEAN_NAME)
        public AuthenticationServiceSelectionPlan authenticationServiceSelectionPlan() {
            return mock(AuthenticationServiceSelectionPlan.class);
        }
    }
}
