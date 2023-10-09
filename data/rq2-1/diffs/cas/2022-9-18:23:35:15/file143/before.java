package org.apereo.cas.support.oauth.web.views;

import org.apereo.cas.configuration.model.support.oauth.OAuthCoreProperties;
import org.apereo.cas.configuration.model.support.oauth.OAuthProperties;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.util.OAuth20Utils;
import org.apereo.cas.ticket.accesstoken.OAuth20AccessToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.bval.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is {@link OAuth20DefaultUserProfileViewRenderer}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Slf4j
@RequiredArgsConstructor
public class OAuth20DefaultUserProfileViewRenderer implements OAuth20UserProfileViewRenderer {
    /**
     * Services manager instance.
     */
    protected final ServicesManager servicesManager;

    private final OAuthProperties oauthProperties;

    @Override
    public ResponseEntity render(final Map<String, Object> model,
                                 final OAuth20AccessToken accessToken,
                                 final HttpServletResponse response) {
        val userProfile = getRenderedUserProfile(model, accessToken, response);
        return renderProfileForModel(userProfile, accessToken, response);
    }

    /**
     * Render profile for model.
     *
     * @param userProfile the user profile
     * @param accessToken the access token
     * @param response    the response
     * @return the string
     */
    protected ResponseEntity renderProfileForModel(final Map<String, Object> userProfile,
                                                   final OAuth20AccessToken accessToken, final HttpServletResponse response) {
        val json = OAuth20Utils.toJson(userProfile);
        LOGGER.debug("Final user profile is [{}]", json);
        return new ResponseEntity<>(json, HttpStatus.OK);
    }

    /**
     * Gets rendered user profile.
     *
     * @param model       the model
     * @param accessToken the access token
     * @param response    the response
     * @return the rendered user profile
     */
    protected Map<String, Object> getRenderedUserProfile(final Map<String, Object> model,
                                                         final OAuth20AccessToken accessToken,
                                                         final HttpServletResponse response) {
        val registeredService = OAuth20Utils.getRegisteredOAuthServiceByClientId(servicesManager, accessToken.getClientId());
        var type = oauthProperties.getCore().getUserProfileViewType();
        if (registeredService != null && StringUtils.isNotBlank(registeredService.getUserProfileViewType())) {
            type = OAuthCoreProperties.UserProfileViewTypes.valueOf(registeredService.getUserProfileViewType().toUpperCase());
        }
        LOGGER.debug("User profile view type for client [{}] is set to [{}]", accessToken.getClientId(), type);

        if (type == OAuthCoreProperties.UserProfileViewTypes.FLAT) {
            val flattened = new LinkedHashMap<String, Object>();
            if (model.containsKey(MODEL_ATTRIBUTE_ATTRIBUTES)) {
                val attributes = Map.class.cast(model.get(MODEL_ATTRIBUTE_ATTRIBUTES));
                flattened.putAll(attributes);
            }
            model.keySet()
                .stream()
                .filter(k -> !k.equalsIgnoreCase(MODEL_ATTRIBUTE_ATTRIBUTES))
                .forEach(k -> flattened.put(k, model.get(k)));
            LOGGER.trace("Flattened user profile attributes with the final model as [{}]", model);
            return flattened;
        }
        return model;
    }
}
