package org.apereo.cas.support.oauth.web.response.callback;

import org.apereo.cas.support.oauth.OAuth20ResponseModeTypes;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.support.oauth.util.OAuth20Utils;
import org.apereo.cas.web.flow.CasWebflowConstants;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This is {@link DefaultOAuth20AuthorizationModelAndViewBuilder}.
 *
 * @author Misagh Moayyed
 * @since 6.4.0
 */
@Slf4j
public class DefaultOAuth20AuthorizationModelAndViewBuilder implements OAuth20AuthorizationModelAndViewBuilder {
    @Override
    public ModelAndView build(final OAuthRegisteredService registeredService,
                              final OAuth20ResponseModeTypes responseMode,
                              final String url, final Map<String, String> parameters) throws Exception {
        val redirectUrl = prepareRedirectUrl(registeredService, url, parameters);
        if (OAuth20Utils.isResponseModeTypeFormPost(registeredService, responseMode)) {
            val model = new LinkedHashMap<String, Object>();
            model.put("originalUrl", redirectUrl);
            model.put("parameters", parameters);
            val mv = new ModelAndView(CasWebflowConstants.VIEW_ID_POST_RESPONSE, model);
            LOGGER.debug("Redirecting to [{}] with model [{}]", mv.getViewName(), mv.getModel());
            return mv;
        }
        if (OAuth20Utils.isResponseModeTypeFragment(registeredService, responseMode)) {
            val urlBuilder = new URIBuilder(redirectUrl);
            val currentParams = urlBuilder.getQueryParams();

            urlBuilder.removeQuery();
            currentParams.removeIf(p -> parameters.containsKey(p.getName()));
            val fragment = parameters.entrySet()
                .stream()
                .map(entry -> entry.getKey() + '=' + entry.getValue())
                .collect(Collectors.joining("&"));
            urlBuilder.setFragment(fragment);
            urlBuilder.setParameters(currentParams);

            val resultUrl = urlBuilder.build().toURL().toExternalForm();
            LOGGER.debug("Redirecting to [{}]", resultUrl);
            val mv = new RedirectView(resultUrl);
            return new ModelAndView(mv);
        }
        val mv = new RedirectView(redirectUrl);
        return new ModelAndView(mv, parameters);
    }

    /**
     * Prepare.
     *
     * @param registeredService the registered service
     * @param redirectUrl       the redirect url
     * @param parameters        the parameters
     * @return the string
     * @throws Exception the exception
     */
    protected String prepareRedirectUrl(final OAuthRegisteredService registeredService,
                                        final String redirectUrl, final Map<String, String> parameters) throws Exception {
        return redirectUrl;
    }
}
