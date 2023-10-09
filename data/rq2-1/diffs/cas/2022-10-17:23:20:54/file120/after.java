package org.apereo.cas.adaptors.duo.web.flow.action;

import org.apereo.cas.adaptors.duo.BaseDuoSecurityTests;
import org.apereo.cas.adaptors.duo.authn.DuoSecurityAuthenticationService;
import org.apereo.cas.adaptors.duo.authn.DuoSecurityCredential;
import org.apereo.cas.adaptors.duo.authn.DuoSecurityMultifactorAuthenticationProvider;
import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.authentication.MultifactorAuthenticationPrincipalResolver;
import org.apereo.cas.authentication.mfa.TestMultifactorAuthenticationProvider;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.mfa.duo.DuoSecurityMultifactorAuthenticationProperties;
import org.apereo.cas.util.MockServletContext;
import org.apereo.cas.util.spring.ApplicationContextProvider;
import org.apereo.cas.web.flow.CasWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.support.WebUtils;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.ViewState;
import org.springframework.webflow.execution.ViewFactory;
import org.springframework.webflow.test.MockFlowExecutionContext;
import org.springframework.webflow.test.MockFlowSession;
import org.springframework.webflow.test.MockRequestContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link DuoSecurityPrepareWebLoginFormActionTests}.
 *
 * @author Misagh Moayyed
 * @since 6.2.0
 */
@SpringBootTest(classes = BaseDuoSecurityTests.SharedTestConfiguration.class,
    properties = {
        "cas.authn.mfa.duo[0].duo-secret-key=1234567890",
        "cas.authn.mfa.duo[0].duo-application-key=abcdefghijklmnop",
        "cas.authn.mfa.duo[0].duo-integration-key=QRSTUVWXYZ",
        "cas.authn.mfa.duo[0].duo-api-host=theapi.duosecurity.com"
    })
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Tag("DuoSecurity")
public class DuoSecurityPrepareWebLoginFormActionTests {
    @Autowired
    private CasConfigurationProperties casProperties;

    @Test
    public void verifyOperation() throws Exception {
        val applicationContext = new StaticApplicationContext();
        applicationContext.refresh();

        ApplicationContextProvider.holdApplicationContext(applicationContext);
        ApplicationContextProvider.registerBeanIntoApplicationContext(applicationContext,
            MultifactorAuthenticationPrincipalResolver.identical(), UUID.randomUUID().toString());
        
        val flowSession = new MockFlowSession(new Flow(CasWebflowConfigurer.FLOW_ID_LOGIN));
        flowSession.setState(new ViewState(flowSession.getDefinitionInternal(), "viewState", mock(ViewFactory.class)));
        val exec = new MockFlowExecutionContext(flowSession);
        val context = new MockRequestContext(exec);

        val request = new MockHttpServletRequest();
        val response = new MockHttpServletResponse();
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, response));

        WebUtils.putServiceIntoFlowScope(context, CoreAuthenticationTestUtils.getWebApplicationService());
        val authentication = CoreAuthenticationTestUtils.getAuthentication();
        WebUtils.putAuthentication(authentication, context);

        val duoService = mock(DuoSecurityAuthenticationService.class);
        when(duoService.getProperties()).thenReturn(casProperties.getAuthn().getMfa().getDuo().get(0));
        val provider = mock(DuoSecurityMultifactorAuthenticationProvider.class);
        when(provider.getId()).thenReturn(DuoSecurityMultifactorAuthenticationProperties.DEFAULT_IDENTIFIER);
        when(provider.getDuoAuthenticationService()).thenReturn(duoService);
        when(provider.matches(anyString())).thenReturn(Boolean.TRUE);

        WebUtils.putCredential(context, new DuoSecurityCredential(authentication.getPrincipal().getId(),
            UUID.randomUUID().toString(), DuoSecurityMultifactorAuthenticationProperties.DEFAULT_IDENTIFIER));
        WebUtils.putMultifactorAuthenticationProviderIdIntoFlowScope(context, provider);
        TestMultifactorAuthenticationProvider.registerProviderIntoApplicationContext(applicationContext, provider);


        val action = new DuoSecurityPrepareWebLoginFormAction();
        val event = action.execute(context);
        assertEquals(CasWebflowConstants.TRANSITION_ID_SUCCESS, event.getId());

    }
}
