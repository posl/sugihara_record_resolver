package org.apereo.cas.config;

import org.apereo.cas.audit.spi.config.CasCoreAuditConfiguration;
import org.apereo.cas.config.support.CasWebApplicationServiceFactoryConfiguration;
import org.apereo.cas.logout.config.CasCoreLogoutConfiguration;
import org.apereo.cas.support.saml.OpenSamlConfigBean;

import net.shibboleth.shared.xml.impl.BasicParserPool;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.io.MarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link CoreSamlConfigurationTests}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@SpringBootTest(classes = CoreSamlConfigurationTests.SharedTestConfiguration.class)
@EnableScheduling
@Tag("SAML2")
public class CoreSamlConfigurationTests {
    @Autowired
    @Qualifier("shibboleth.VelocityEngine")
    protected VelocityEngine velocityEngineFactoryBean;

    @Autowired
    @Qualifier(OpenSamlConfigBean.DEFAULT_BEAN_NAME)
    protected OpenSamlConfigBean openSamlConfigBean;

    @Autowired
    @Qualifier("shibboleth.ParserPool")
    protected BasicParserPool parserPool;

    @Autowired
    @Qualifier("shibboleth.BuilderFactory")
    protected XMLObjectBuilderFactory builderFactory;

    @Autowired
    @Qualifier("shibboleth.MarshallerFactory")
    protected MarshallerFactory marshallerFactory;

    @Autowired
    @Qualifier("shibboleth.UnmarshallerFactory")
    protected UnmarshallerFactory unmarshallerFactory;

    @Test
    public void verify() {
        assertNotNull(velocityEngineFactoryBean);
        assertNotNull(openSamlConfigBean);
        assertNotNull(parserPool);
        assertNotNull(builderFactory);
        assertNotNull(marshallerFactory);
        assertNotNull(unmarshallerFactory);
    }

    @ImportAutoConfiguration({
        RefreshAutoConfiguration.class,
        AopAutoConfiguration.class
    })
    @SpringBootConfiguration
    @Import({
        CasCoreAuthenticationConfiguration.class,
        CasCoreWebConfiguration.class,
        CasWebApplicationServiceFactoryConfiguration.class,
        CasCoreServicesAuthenticationConfiguration.class,
        CasCoreAuthenticationPrincipalConfiguration.class,
        CasCoreAuthenticationPolicyConfiguration.class,
        CasCoreAuthenticationMetadataConfiguration.class,
        CasCoreAuthenticationSupportConfiguration.class,
        CasCoreAuthenticationHandlersConfiguration.class,
        CasCoreTicketIdGeneratorsConfiguration.class,
        CasCoreAuditConfiguration.class,
        CasCoreAuthenticationServiceSelectionStrategyConfiguration.class,
        CasCoreHttpConfiguration.class,
        CasCoreUtilConfiguration.class,
        CoreSamlConfiguration.class,
        CasCoreTicketCatalogConfiguration.class,
        CasCoreTicketsSerializationConfiguration.class,
        CasCoreTicketsConfiguration.class,
        CasPersonDirectoryConfiguration.class,
        CasCoreNotificationsConfiguration.class,
        CasCoreServicesConfiguration.class,
        CasCoreLogoutConfiguration.class,
        CasCoreConfiguration.class
    })
    public static class SharedTestConfiguration {
    }
}
