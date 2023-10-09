package org.apereo.cas.support.oauth.util;

import org.apereo.cas.AbstractOAuth20Tests;
import org.apereo.cas.CasProtocolConstants;
import org.apereo.cas.services.FullRegexRegisteredServiceMatchingStrategy;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.OAuth20GrantTypes;
import org.apereo.cas.support.oauth.OAuth20ResponseModeTypes;
import org.apereo.cas.support.oauth.OAuth20ResponseTypes;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.support.oauth.web.OAuth20RequestParameterResolver;
import org.apereo.cas.ticket.OAuth20Token;
import org.apereo.cas.util.CollectionUtils;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jee.context.JEEContext;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link OAuth20UtilsTests}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@Tag("OAuth")
public class OAuth20UtilsTests extends AbstractOAuth20Tests {
    @Test
    public void verifyRequestHeaderBad() {
        assertNull(OAuth20Utils.getClientIdFromAuthenticatedProfile(new CommonProfile()));
    }

    @Test
    public void verifyUnauthzView() {
        val mv = OAuth20Utils.produceUnauthorizedErrorView();
        assertEquals(HttpStatus.UNAUTHORIZED, mv.getStatus());
    }

    @Test
    public void verifyNoClientId() {
        assertNull(OAuth20Utils.getRegisteredOAuthServiceByClientId(mock(ServicesManager.class), null));
    }

    @Test
    public void verifyRequestParams() {
        val request = new MockHttpServletRequest();
        val response = new MockHttpServletResponse();
        val context = new JEEContext(request, response);
        request.addParameter("attr1", "value1");
        request.addParameter("attr2", "value2", "value3");
        assertFalse(oauthRequestParameterResolver.resolveRequestParameters(List.of("attr1", "attr2"), context).isEmpty());
    }

    @Test
    public void verifyRequestParam() throws Exception {
        val request = new MockHttpServletRequest();
        val response = new MockHttpServletResponse();
        val context = new JEEContext(request, response);
        request.addParameter("attr1", "value1");
        request.addParameter("attr2", "value2", "value3");
        assertFalse(oauthRequestParameterResolver.resolveRequestParameter(context, "attr1", String.class).isEmpty());
        assertFalse(oauthRequestParameterResolver.resolveRequestParameter(context, "attr2", List.class).isEmpty());
        assertFalse(oauthRequestParameterResolver.resolveRequestParameter(context, "attr2", String[].class).isEmpty());
    }

    @Test
    public void verifyRequestParamJwt() throws Exception {
        val request = new MockHttpServletRequest();
        val response = new MockHttpServletResponse();
        val context = new JEEContext(request, response);

        val claims = new JWTClaimsSet.Builder().subject("cas")
            .claim("scope", new String[]{"openid", "profile"})
            .claim("response", "code")
            .claim("client_id", List.of("client1", "client2"))
            .build();
        val jwt = new PlainJWT(claims);
        val jwtString = jwt.serialize();
        request.removeAllParameters();
        request.addParameter(OAuth20Constants.REQUEST, jwtString);

        assertFalse(oauthRequestParameterResolver.resolveRequestParameter(context, "response", String.class).isEmpty());
        assertFalse(oauthRequestParameterResolver.resolveRequestParameter(context, "client_id", List.class).isEmpty());
        assertFalse(oauthRequestParameterResolver.resolveRequestParameter(context, "scope", String[].class).isEmpty());
    }

    @Test
    public void verifyScopes() {
        val request = new MockHttpServletRequest();
        val response = new MockHttpServletResponse();
        val context = new JEEContext(request, response);
        request.addParameter("scope", "openid", "profile");
        assertFalse(oauthRequestParameterResolver.resolveRequestedScopes(context).isEmpty());
        assertTrue(oauthRequestParameterResolver.resolveRequestedScopes(new JEEContext(new MockHttpServletRequest(), response)).isEmpty());
    }

    @Test
    public void verifyPostResponse() {
        val registeredService = new OAuthRegisteredService();
        registeredService.setClientId("clientid");
        registeredService.setResponseType("post");
        assertTrue(OAuth20Utils.isResponseModeTypeFormPost(registeredService, OAuth20ResponseModeTypes.NONE));
        assertTrue(OAuth20Utils.isResponseModeType("form_post", OAuth20ResponseModeTypes.FORM_POST));
    }

    @Test
    public void verifyGrants() {
        val request = new MockHttpServletRequest();
        val response = new MockHttpServletResponse();
        val context = new JEEContext(request, response);
        request.addParameter(OAuth20Constants.GRANT_TYPE, OAuth20GrantTypes.CLIENT_CREDENTIALS.getType());
        val registeredService = new OAuthRegisteredService();
        registeredService.setClientId("clientid");
        registeredService.setSupportedGrantTypes(CollectionUtils.wrapHashSet(OAuth20GrantTypes.CLIENT_CREDENTIALS.getType()));
        assertTrue(oauthRequestParameterResolver.isAuthorizedGrantTypeForService(context, registeredService));
        assertTrue(OAuth20RequestParameterResolver.isAuthorizedGrantTypeForService(
            OAuth20GrantTypes.PASSWORD.getType(), new OAuthRegisteredService()));
    }

    @Test
    public void verifyCheckCallbackValid() {
        val registeredService = new OAuthRegisteredService();
        registeredService.setServiceId("http://test.org/.*");
        registeredService.setMatchingStrategy(null);
        assertFalse(OAuth20Utils.checkCallbackValid(registeredService, "http://test.org/cas"));
        registeredService.setMatchingStrategy(new FullRegexRegisteredServiceMatchingStrategy());
        assertTrue(OAuth20Utils.checkCallbackValid(registeredService, "http://test.org/cas"));
        assertFalse(OAuth20Utils.checkCallbackValid(registeredService, "http://test2.org/cas"));
    }

    

    @Test
    public void verifyServiceHeader() {
        val request = new MockHttpServletRequest();
        val response = new MockHttpServletResponse();
        val context = new JEEContext(request, response);
        request.addHeader("X-".concat(CasProtocolConstants.PARAMETER_SERVICE), RegisteredServiceTestUtils.CONST_TEST_URL);
        val result = OAuth20Utils.getServiceRequestHeaderIfAny(context);
        assertNotNull(result);
    }

    @Test
    public void verifyUserInfoClaims() throws Exception {
        val request = new MockHttpServletRequest();
        val response = new MockHttpServletResponse();
        val context = new JEEContext(request, response);
        val claims = "\"userinfo\": {\"given_name\": {\"essential\": true}}";
        request.addParameter(OAuth20Constants.CLAIMS, claims);
        val result = oauthRequestParameterResolver.resolveRequestClaims(context);
        assertFalse(result.isEmpty());
        val token = mock(OAuth20Token.class);
        when(token.getClaims()).thenReturn(Map.of("userinfo", Map.of("givenName", "CAS")));
        assertFalse(OAuth20Utils.parseUserInfoRequestClaims(token).isEmpty());
    }


    @Test
    public void verifyIsAuthorizedResponseTypeForService() {
        val request = new MockHttpServletRequest();
        request.addParameter(OAuth20Constants.RESPONSE_TYPE, OAuth20ResponseTypes.ID_TOKEN.getType());
        val response = new MockHttpServletResponse();
        val context = new JEEContext(request, response);
        val registeredService = new OAuthRegisteredService();
        val supportedResponseTypes = new HashSet<String>();

        registeredService.setSupportedResponseTypes(supportedResponseTypes);
        assertTrue(oauthRequestParameterResolver.isAuthorizedResponseTypeForService(context, registeredService));

        supportedResponseTypes.add(OAuth20ResponseTypes.IDTOKEN_TOKEN.getType());
        registeredService.setSupportedResponseTypes(supportedResponseTypes);
        assertFalse(oauthRequestParameterResolver.isAuthorizedResponseTypeForService(context, registeredService));

        supportedResponseTypes.add(OAuth20ResponseTypes.ID_TOKEN.getType());
        registeredService.setSupportedResponseTypes(supportedResponseTypes);
        assertTrue(oauthRequestParameterResolver.isAuthorizedResponseTypeForService(context, registeredService));
    }
}
