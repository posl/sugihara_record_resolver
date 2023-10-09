package org.apereo.cas.adaptors.swivel.web.flow;

import org.apereo.cas.adaptors.swivel.BaseSwivelAuthenticationTests;
import org.apereo.cas.adaptors.swivel.SwivelTokenCredential;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.AuthenticationResultBuilder;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.support.WebUtils;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.binding.message.MessageContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.webflow.context.ExternalContextHolder;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.execution.Action;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.RequestContextHolder;
import org.springframework.webflow.test.MockParameterMap;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link SwivelAuthenticationWebflowActionTests}.
 *
 * @author Misagh Moayyed
 * @since 6.3.0
 * @deprecated Since 6.6.
 */
@Tag("WebflowMfaActions")
@Deprecated(since = "6.6")
@SpringBootTest(classes = BaseSwivelAuthenticationTests.SharedTestConfiguration.class,
    properties = {
        "cas.authn.mfa.swivel.swivel-url=http://localhost:9191",
        "cas.authn.mfa.swivel.shared-secret=$ecret",
        "cas.authn.mfa.swivel.ignore-ssl-errors=true",
        "cas.authn.mfa.swivel.trusted-device-enabled=true",
        "cas.authn.mfa.trusted.core.device-registration-enabled=true"
    })
public class SwivelAuthenticationWebflowActionTests {
    @Autowired
    @Qualifier(CasWebflowConstants.ACTION_ID_SWIVEL_AUTHENTICATION)
    private Action swivelAuthenticationWebflowAction;

    @Test
    public void verifyAction() throws Exception {
        val request = new MockHttpServletRequest();
        val response = new MockHttpServletResponse();
        val context = mock(RequestContext.class);
        when(context.getMessageContext()).thenReturn(mock(MessageContext.class));
        when(context.getRequestParameters()).thenReturn(new MockParameterMap());
        when(context.getFlowScope()).thenReturn(new LocalAttributeMap<>());
        when(context.getConversationScope()).thenReturn(new LocalAttributeMap<>());
        when(context.getRequestScope()).thenReturn(new LocalAttributeMap<>());
        when(context.getFlashScope()).thenReturn(new LocalAttributeMap<>());
        val external = new ServletExternalContext(new MockServletContext(), request, response);
        when(context.getExternalContext()).thenReturn(external);

        RequestContextHolder.setRequestContext(context);
        ExternalContextHolder.setExternalContext(external);

        val authn = RegisteredServiceTestUtils.getAuthentication("casuser");
        val builder = mock(AuthenticationResultBuilder.class);
        when(builder.getInitialAuthentication()).thenReturn(Optional.of(authn));
        when(builder.collect(any(Authentication.class))).thenReturn(builder);

        WebUtils.putAuthenticationResultBuilder(builder, context);
        WebUtils.putAuthentication(authn, context);
        WebUtils.putCredential(context, new SwivelTokenCredential("token"));
        assertEquals(CasWebflowConstants.TRANSITION_ID_ERROR, swivelAuthenticationWebflowAction.execute(context).getId());
    }

}
