package org.apereo.cas.web.flow.actions;

import org.apereo.cas.support.saml.SamlProtocolConstants;
import org.apereo.cas.util.HttpRequestUtils;
import org.apereo.cas.util.MockServletContext;
import org.apereo.cas.web.BaseDelegatedAuthenticationTests;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.DelegationWebflowUtils;
import org.apereo.cas.web.flow.actions.logout.DelegatedAuthenticationClientLogoutRequest;
import org.apereo.cas.web.support.WebUtils;

import lombok.val;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.pac4j.core.client.Clients;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.logout.processor.LogoutProcessor;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.logout.processor.SAML2LogoutProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.execution.Action;
import org.springframework.webflow.test.MockRequestContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link DelegatedAuthenticationClientFinishLogoutActionTests}.
 *
 * @author Misagh Moayyed
 * @since 6.4.0
 */
@SpringBootTest(classes = BaseDelegatedAuthenticationTests.SharedTestConfiguration.class)
@Tag("Delegation")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DelegatedAuthenticationClientFinishLogoutActionTests {
    @Autowired
    @Qualifier(CasWebflowConstants.ACTION_ID_DELEGATED_AUTHENTICATION_CLIENT_FINISH_LOGOUT)
    private Action delegatedAuthenticationClientFinishLogoutAction;
    
    @Autowired
    @Qualifier("builtClients")
    private Clients builtClients;

    @Test
    @Order(1)
    public void verifyOperationWithRedirect() throws Exception {
        val context = new MockRequestContext();
        val request = new MockHttpServletRequest();
        request.addHeader(HttpRequestUtils.USER_AGENT_HEADER, "Mozilla/5.0 (Windows NT 10.0; WOW64)");
        val response = new MockHttpServletResponse();
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, response));
        DelegationWebflowUtils.putDelegatedAuthenticationClientName(context, "SAML2Client");
        WebUtils.putLogoutRedirectUrl(context, "https://google.com");

        val logoutRequest = DelegatedAuthenticationClientLogoutRequest.builder().status(200).build();
        DelegationWebflowUtils.putDelegatedAuthenticationLogoutRequest(context, logoutRequest);

        val result = delegatedAuthenticationClientFinishLogoutAction.execute(context);
        assertNull(result);
        val samlClient = (SAML2Client) builtClients.findClient("SAML2Client").get();
        val logoutProcessor = (SAML2LogoutProcessor) samlClient.getLogoutProcessor();
        assertEquals("https://google.com", logoutProcessor.getPostLogoutURL());
        assertNull(WebUtils.getLogoutRedirectUrl(context, String.class));
    }

    @Test
    @Order(1)
    public void verifyOperationNoLogoutRedirectUrl() throws Exception {
        val context = new MockRequestContext();
        val request = new MockHttpServletRequest();
        request.addHeader(HttpRequestUtils.USER_AGENT_HEADER, "Mozilla/5.0 (Windows NT 10.0; WOW64)");
        val response = new MockHttpServletResponse();
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, response));
        DelegationWebflowUtils.putDelegatedAuthenticationClientName(context, "SAML2Client");
        val samlClient = (SAML2Client) builtClients.findClient("SAML2Client").get();
        val logoutProcessor = (SAML2LogoutProcessor) samlClient.getLogoutProcessor();
        logoutProcessor.setPostLogoutURL("https://google.com");
        
        val logoutRequest = DelegatedAuthenticationClientLogoutRequest.builder().status(200).build();
        DelegationWebflowUtils.putDelegatedAuthenticationLogoutRequest(context, logoutRequest);
        
        val result = delegatedAuthenticationClientFinishLogoutAction.execute(context);
        assertNull(result);
        assertEquals("https://google.com", logoutProcessor.getPostLogoutURL());
        assertNull(WebUtils.getLogoutRedirectUrl(context, String.class));
    }

    @Test
    @Order(1)
    public void verifyOperationWithRelay() throws Exception {
        val context = new MockRequestContext();
        val request = new MockHttpServletRequest();
        request.addHeader(HttpRequestUtils.USER_AGENT_HEADER, "Mozilla/5.0 (Windows NT 10.0; WOW64)");
        request.addParameter(SamlProtocolConstants.PARAMETER_SAML_RELAY_STATE, "SAML2Client");
        val response = new MockHttpServletResponse();
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, response));
        val result = delegatedAuthenticationClientFinishLogoutAction.execute(context);
        assertNull(result);
    }

    @Test
    @Order(1000)
    public void verifyOperationFailsWithError() throws Exception {
        val context = new MockRequestContext();
        val request = new MockHttpServletRequest();
        request.addHeader(HttpRequestUtils.USER_AGENT_HEADER, "Mozilla/5.0 (Windows NT 10.0; WOW64)");
        request.addParameter(SamlProtocolConstants.PARAMETER_SAML_RELAY_STATE, "SAML2Client");
        val response = new MockHttpServletResponse();
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, response));
        val samlClient = (SAML2Client) builtClients.findClient("SAML2Client").get();
        val handler = mock(LogoutProcessor.class);
        when(handler.processLogout(any(), any())).thenReturn(new FoundAction("https://google.com"));
        samlClient.setLogoutProcessor(handler);
        val result = delegatedAuthenticationClientFinishLogoutAction.execute(context);
        assertNull(result);
        assertEquals(HttpStatus.FOUND.value(), response.getStatus());
        assertEquals("https://google.com", response.getHeader("Location"));
    }
}
