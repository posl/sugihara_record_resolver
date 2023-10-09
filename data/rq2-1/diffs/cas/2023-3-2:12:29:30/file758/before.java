package org.apereo.cas.adaptors.radius.authentication.handler.support;

import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.config.CasCoreAuthenticationConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationPrincipalConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationServiceSelectionStrategyConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationSupportConfiguration;
import org.apereo.cas.config.CasCoreConfiguration;
import org.apereo.cas.config.CasCoreHttpConfiguration;
import org.apereo.cas.config.CasCoreMultifactorAuthenticationConfiguration;
import org.apereo.cas.config.CasCoreNotificationsConfiguration;
import org.apereo.cas.config.CasCoreServicesConfiguration;
import org.apereo.cas.config.CasCoreTicketCatalogConfiguration;
import org.apereo.cas.config.CasCoreTicketIdGeneratorsConfiguration;
import org.apereo.cas.config.CasCoreTicketsConfiguration;
import org.apereo.cas.config.CasCoreUtilConfiguration;
import org.apereo.cas.config.CasCoreWebConfiguration;
import org.apereo.cas.config.CasPersonDirectoryTestConfiguration;
import org.apereo.cas.config.RadiusConfiguration;
import org.apereo.cas.config.support.CasWebApplicationServiceFactoryConfiguration;
import org.apereo.cas.logout.config.CasCoreLogoutConfiguration;
import org.apereo.cas.web.config.CasCookieConfiguration;
import org.apereo.cas.web.flow.config.CasCoreWebflowConfiguration;
import org.apereo.cas.web.flow.config.CasMultifactorAuthenticationWebflowConfiguration;
import org.apereo.cas.web.flow.config.CasWebflowContextConfiguration;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link RadiusAuthenticationHandlerTests}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0
 */
@SpringBootTest(classes = {
    RadiusConfiguration.class,
    RefreshAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    CasCoreAuthenticationPrincipalConfiguration.class,
    CasCoreAuthenticationSupportConfiguration.class,
    CasCoreMultifactorAuthenticationConfiguration.class,
    CasCoreAuthenticationServiceSelectionStrategyConfiguration.class,
    CasCoreWebConfiguration.class,
    CasMultifactorAuthenticationWebflowConfiguration.class,
    CasCoreAuthenticationConfiguration.class,
    CasCoreWebflowConfiguration.class,
    CasWebflowContextConfiguration.class,
    CasWebApplicationServiceFactoryConfiguration.class,
    CasCoreTicketIdGeneratorsConfiguration.class,
    CasCoreTicketCatalogConfiguration.class,
    CasCoreNotificationsConfiguration.class,
    CasCoreServicesConfiguration.class,
    CasCoreTicketsConfiguration.class,
    CasCoreLogoutConfiguration.class,
    CasCookieConfiguration.class,
    CasCoreHttpConfiguration.class,
    CasCoreUtilConfiguration.class,
    CasPersonDirectoryTestConfiguration.class,
    CasCoreConfiguration.class
}, properties = {
    "cas.authn.radius.server.protocol=PAP",
    "cas.authn.radius.client.shared-secret=testing123",
    "cas.authn.radius.client.inet-address=localhost"
})
@Tag("Radius")
@EnabledOnOs(OS.LINUX)
public class RadiusAuthenticationHandlerTests {
    @Autowired
    @Qualifier("radiusAuthenticationHandler")
    private AuthenticationHandler radiusAuthenticationHandler;

    @Test
    public void verifyOperation() throws Exception {
        val c = CoreAuthenticationTestUtils.getCredentialsWithDifferentUsernameAndPassword("casuser", "Mellon");
        val result = radiusAuthenticationHandler.authenticate(c, mock(Service.class));
        assertNotNull(result);
    }
}
