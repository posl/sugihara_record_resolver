package org.apereo.cas.token;

import org.apereo.cas.authentication.ProtocolAttributeEncoder;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.config.CasCoreAuthenticationPrincipalConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationServiceSelectionStrategyConfiguration;
import org.apereo.cas.config.CasCoreConfiguration;
import org.apereo.cas.config.CasCoreHttpConfiguration;
import org.apereo.cas.config.CasCoreNotificationsConfiguration;
import org.apereo.cas.config.CasCoreServicesConfiguration;
import org.apereo.cas.config.CasCoreTicketCatalogConfiguration;
import org.apereo.cas.config.CasCoreTicketIdGeneratorsConfiguration;
import org.apereo.cas.config.CasCoreTicketsConfiguration;
import org.apereo.cas.config.CasCoreTicketsSerializationConfiguration;
import org.apereo.cas.config.CasCoreUtilConfiguration;
import org.apereo.cas.config.CasCoreWebConfiguration;
import org.apereo.cas.config.CasDefaultServiceTicketIdGeneratorsConfiguration;
import org.apereo.cas.config.CasPersonDirectoryTestConfiguration;
import org.apereo.cas.config.CasRegisteredServicesTestConfiguration;
import org.apereo.cas.config.TokenCoreComponentSerializationConfiguration;
import org.apereo.cas.config.TokenCoreConfiguration;
import org.apereo.cas.config.support.CasWebApplicationServiceFactoryConfiguration;
import org.apereo.cas.services.BaseRegisteredService;
import org.apereo.cas.services.DefaultRegisteredServiceProperty;
import org.apereo.cas.services.RegisteredServiceProperty;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.validation.TicketValidator;

import lombok.val;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * This is {@link BaseJwtTokenTicketBuilderTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@SpringBootTest(classes = {
    RefreshAutoConfiguration.class,
    BaseJwtTokenTicketBuilderTests.TokenTicketBuilderTestConfiguration.class,
    TokenCoreConfiguration.class,
    TokenCoreComponentSerializationConfiguration.class,
    CasCoreTicketsConfiguration.class,
    CasCoreNotificationsConfiguration.class,
    CasCoreServicesConfiguration.class,
    CasCoreUtilConfiguration.class,
    CasRegisteredServicesTestConfiguration.class,
    CasCoreTicketCatalogConfiguration.class,
    CasCoreTicketsSerializationConfiguration.class,
    CasCoreTicketIdGeneratorsConfiguration.class,
    CasCoreHttpConfiguration.class,
    CasCoreAuthenticationPrincipalConfiguration.class,
    CasCoreWebConfiguration.class,
    CasWebApplicationServiceFactoryConfiguration.class,
    CasCoreConfiguration.class,
    CasPersonDirectoryTestConfiguration.class,
    CasCoreAuthenticationServiceSelectionStrategyConfiguration.class,
    CasDefaultServiceTicketIdGeneratorsConfiguration.class
})
public abstract class BaseJwtTokenTicketBuilderTests {
    @Autowired
    @Qualifier("tokenTicketBuilder")
    protected TokenTicketBuilder tokenTicketBuilder;

    @Autowired
    @Qualifier("tokenCipherExecutor")
    protected CipherExecutor tokenCipherExecutor;

    @Autowired
    @Qualifier("tokenTicketJwtBuilder")
    protected JwtBuilder tokenTicketJwtBuilder;

    @Autowired
    @Qualifier(ServicesManager.BEAN_NAME)
    protected ServicesManager servicesManager;

    @TestConfiguration(value = "TokenTicketBuilderTestConfiguration", proxyBeanMethods = false)
    public static class TokenTicketBuilderTestConfiguration implements InitializingBean {
        @Autowired
        @Qualifier("servicesManager")
        private ServicesManager servicesManager;

        @Override
        public void afterPropertiesSet() {
            servicesManager.save(RegisteredServiceTestUtils.getRegisteredService("https://cas.example.org.+"));
            servicesManager.save(createRegisteredService("https://jwt.example.org/cas.*", true, true));
            servicesManager.save(createRegisteredService("https://jwt.no-encryption-key.example.org/cas.*", true, false));
        }

        @Bean
        public TicketValidator tokenTicketValidator() {
            val validator = mock(TicketValidator.class);
            val principal = PrincipalFactoryUtils.newPrincipalFactory().createPrincipal("casuser",
                CollectionUtils.wrap("name", List.of("value"),
                    ProtocolAttributeEncoder.encodeAttribute("custom:name"), CollectionUtils.wrapList("custom:value")));
            when(validator.validate(anyString(), anyString()))
                .thenReturn(TicketValidator.ValidationResult.builder().principal(principal).build());
            return validator;

        }

        private BaseRegisteredService createRegisteredService(final String id, final boolean hasSigningKey, final boolean hasEncryptionKey) {
            val registeredService = RegisteredServiceTestUtils.getRegisteredService(id);

            if (hasSigningKey) {
                val signingKey = new DefaultRegisteredServiceProperty();
                signingKey.addValue("pR3Vizkn5FSY5xCg84cIS4m-b6jomamZD68C8ash-TlNmgGPcoLgbgquxHPoi24tRmGpqHgM4mEykctcQzZ-Xg");
                registeredService.getProperties().put(
                    RegisteredServiceProperty.RegisteredServiceProperties.TOKEN_AS_SERVICE_TICKET_SIGNING_KEY.getPropertyName(), signingKey);
            }
            if (hasEncryptionKey) {
                val encKey = new DefaultRegisteredServiceProperty();
                encKey.addValue("0KVXaN-nlXafRUwgsr3H_l6hkufY7lzoTy7OVI5pN0E");
                registeredService.getProperties().put(
                    RegisteredServiceProperty.RegisteredServiceProperties.TOKEN_AS_SERVICE_TICKET_ENCRYPTION_KEY.getPropertyName(), encKey);

                servicesManager.save(registeredService);
            }
            return registeredService;
        }
    }
}
