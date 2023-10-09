package org.apereo.cas.support.oauth.web.response.accesstoken;

import org.apereo.cas.authentication.DefaultAuthenticationBuilder;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.Beans;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.OAuth20ResponseTypes;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.support.oauth.validator.token.device.InvalidOAuth20DeviceTokenException;
import org.apereo.cas.support.oauth.validator.token.device.ThrottledOAuth20DeviceUserCodeApprovalException;
import org.apereo.cas.support.oauth.validator.token.device.UnapprovedOAuth20DeviceUserCodeException;
import org.apereo.cas.support.oauth.web.response.accesstoken.ext.AccessTokenRequestContext;
import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.accesstoken.OAuth20AccessToken;
import org.apereo.cas.ticket.accesstoken.OAuth20AccessTokenFactory;
import org.apereo.cas.ticket.device.OAuth20DeviceToken;
import org.apereo.cas.ticket.device.OAuth20DeviceTokenFactory;
import org.apereo.cas.ticket.device.OAuth20DeviceUserCode;
import org.apereo.cas.ticket.device.OAuth20DeviceUserCodeFactory;
import org.apereo.cas.ticket.refreshtoken.OAuth20RefreshToken;
import org.apereo.cas.ticket.refreshtoken.OAuth20RefreshTokenFactory;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.LoggingUtils;
import org.apereo.cas.util.function.FunctionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.lambda.Unchecked;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Optional;

/**
 * This is {@link OAuth20DefaultTokenGenerator}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Slf4j
@RequiredArgsConstructor
public class OAuth20DefaultTokenGenerator implements OAuth20TokenGenerator {
    /**
     * The Access token factory.
     */
    protected final OAuth20AccessTokenFactory accessTokenFactory;

    /**
     * The device token factory.
     */
    protected final OAuth20DeviceTokenFactory deviceTokenFactory;

    /**
     * The device user code factory.
     */
    protected final OAuth20DeviceUserCodeFactory deviceUserCodeFactory;

    /**
     * The refresh token factory.
     */
    protected final OAuth20RefreshTokenFactory refreshTokenFactory;

    /**
     * The CAS service.
     */
    protected final TicketRegistry ticketRegistry;

    /**
     * CAS configuration settings.
     */
    protected final CasConfigurationProperties casProperties;

    private static OAuth20TokenGeneratedResult generateAccessTokenResult(
        final AccessTokenRequestContext holder,
        final Pair<OAuth20AccessToken, OAuth20RefreshToken> pair) {
        return OAuth20TokenGeneratedResult.builder()
            .registeredService(holder.getRegisteredService())
            .accessToken(pair.getKey())
            .refreshToken(pair.getValue())
            .grantType(holder.getGrantType())
            .responseType(holder.getResponseType())
            .build();
    }

    @Override
    public OAuth20TokenGeneratedResult generate(final AccessTokenRequestContext holder) throws Exception {
        if (OAuth20ResponseTypes.DEVICE_CODE.equals(holder.getResponseType())) {
            return generateAccessTokenOAuthDeviceCodeResponseType(holder);
        }

        val pair = generateAccessTokenOAuthGrantTypes(holder);
        return generateAccessTokenResult(holder, pair);
    }

    /**
     * Generate access token OAuth device code response type OAuth token generated result.
     *
     * @param holder the holder
     * @return the OAuth token generated result
     * @throws Exception the exception
     */
    protected OAuth20TokenGeneratedResult generateAccessTokenOAuthDeviceCodeResponseType(final AccessTokenRequestContext holder) throws Exception {
        val deviceCode = holder.getDeviceCode();

        if (StringUtils.isNotBlank(deviceCode)) {
            val deviceCodeTicket = getDeviceTokenFromTicketRegistry(deviceCode);
            val deviceUserCode = getDeviceUserCodeFromRegistry(deviceCodeTicket);

            if (deviceUserCode.isUserCodeApproved()) {
                LOGGER.debug("Provided user code [{}] linked to device code [{}] is approved", deviceCodeTicket.getId(), deviceCode);
                ticketRegistry.deleteTicket(deviceCode);

                val deviceResult = AccessTokenRequestContext.builder()
                    .service(holder.getService())
                    .authentication(holder.getAuthentication())
                    .registeredService(holder.getRegisteredService())
                    .ticketGrantingTicket(holder.getTicketGrantingTicket())
                    .grantType(holder.getGrantType())
                    .scopes(new LinkedHashSet<>(0))
                    .responseType(holder.getResponseType())
                    .generateRefreshToken(holder.getRegisteredService() != null && holder.isGenerateRefreshToken())
                    .build();

                val ticketPair = generateAccessTokenOAuthGrantTypes(deviceResult);
                return generateAccessTokenResult(deviceResult, ticketPair);
            }

            if (deviceCodeTicket.getLastTimeUsed() != null) {
                val interval = Beans.newDuration(casProperties.getAuthn().getOauth().getDeviceToken().getRefreshInterval()).getSeconds();
                val shouldSlowDown = deviceCodeTicket.getLastTimeUsed().plusSeconds(interval)
                    .isAfter(ZonedDateTime.now(ZoneOffset.UTC));
                if (shouldSlowDown) {
                    LOGGER.error("Request for user code approval is greater than the configured refresh interval of [{}] second(s)", interval);
                    throw new ThrottledOAuth20DeviceUserCodeApprovalException(deviceCodeTicket.getId());
                }
            }
            deviceCodeTicket.update();
            ticketRegistry.updateTicket(deviceCodeTicket);
            LOGGER.error("Provided user code [{}] linked to device code [{}] is NOT approved yet", deviceCodeTicket.getId(), deviceCode);
            throw new UnapprovedOAuth20DeviceUserCodeException(deviceCodeTicket.getId());
        }

        val deviceTokens = createDeviceTokensInTicketRegistry(holder);
        return OAuth20TokenGeneratedResult.builder()
            .responseType(holder.getResponseType())
            .registeredService(holder.getRegisteredService())
            .deviceCode(deviceTokens.getLeft().getId())
            .userCode(deviceTokens.getValue().getId())
            .build();
    }

    protected Pair<OAuth20AccessToken, OAuth20RefreshToken> generateAccessTokenOAuthGrantTypes(
        final AccessTokenRequestContext holder) throws Exception {
        LOGGER.debug("Creating access token for [{}]", holder.getService());
        val authnBuilder = DefaultAuthenticationBuilder
            .newInstance(holder.getAuthentication())
            .setAuthenticationDate(ZonedDateTime.now(ZoneOffset.UTC))
            .addAttribute(OAuth20Constants.GRANT_TYPE, holder.getGrantType().toString())
            .addAttribute(OAuth20Constants.SCOPE, holder.getScopes());

        val clientId = Optional.ofNullable(holder.getRegisteredService())
            .map(OAuthRegisteredService::getClientId).orElse(StringUtils.EMPTY);
        val requestedClaims = holder.getClaims().getOrDefault(OAuth20Constants.CLAIMS_USERINFO, new HashMap<>());
        requestedClaims.forEach(authnBuilder::addAttribute);

        FunctionUtils.doIfNotNull(holder.getDpop(),
            unused -> authnBuilder.addAttribute(OAuth20Constants.DPOP, holder.getDpop()));
        FunctionUtils.doIfNotNull(holder.getDpopConfirmation(),
            unused -> authnBuilder.addAttribute(OAuth20Constants.DPOP_CONFIRMATION, holder.getDpopConfirmation()));
        
        val authentication = authnBuilder.build();
        LOGGER.debug("Creating access token for [{}]", holder);
        val ticketGrantingTicket = holder.getTicketGrantingTicket();
        val accessToken = accessTokenFactory.create(holder.getService(),
            authentication, ticketGrantingTicket, holder.getScopes(),
            Optional.ofNullable(holder.getToken()).map(Ticket::getId).orElse(null),
            clientId,
            holder.getClaims(),
            holder.getResponseType(),
            holder.getGrantType());

        LOGGER.debug("Created access token [{}]", accessToken);
        addTicketToRegistry(accessToken, ticketGrantingTicket);
        LOGGER.debug("Added access token [{}] to registry", accessToken);

        updateOAuthCode(holder, accessToken);

        val refreshToken = FunctionUtils.doIf(holder.isGenerateRefreshToken(),
            Unchecked.supplier(() -> generateRefreshToken(holder, accessToken)),
            () -> {
                LOGGER.debug("Service [{}] is not able/allowed to receive refresh tokens", holder.getService());
                return null;
            }).get();

        return Pair.of(accessToken, refreshToken);
    }

    protected void updateOAuthCode(final AccessTokenRequestContext holder, final OAuth20AccessToken accessToken) throws Exception {
        if (holder.isRefreshToken()) {
            val refreshToken = (OAuth20RefreshToken) holder.getToken();
            refreshToken.getAccessTokens().add(accessToken.getId());
            ticketRegistry.updateTicket(refreshToken);
        } else if (holder.isCodeToken()) {
            val codeState = Ticket.class.cast(holder.getToken());
            codeState.update();

            if (holder.getToken().isExpired()) {
                ticketRegistry.deleteTicket(holder.getToken().getId());
            } else {
                ticketRegistry.updateTicket(holder.getToken());
            }
            ticketRegistry.updateTicket(holder.getTicketGrantingTicket());
        }
    }

    /**
     * Add ticket to registry.
     *
     * @param ticket               the ticket
     * @param ticketGrantingTicket the ticket granting ticket
     * @throws Exception the exception
     */
    protected void addTicketToRegistry(final Ticket ticket, final TicketGrantingTicket ticketGrantingTicket) throws Exception {
        LOGGER.debug("Adding ticket [{}] to registry", ticket);
        ticketRegistry.addTicket(ticket);
        if (ticketGrantingTicket != null) {
            LOGGER.debug("Updating parent ticket-granting ticket [{}]", ticketGrantingTicket);
            ticketRegistry.updateTicket(ticketGrantingTicket);
        }
    }

    /**
     * Add ticket to registry.
     *
     * @param ticket the ticket
     * @throws Exception the exception
     */
    protected void addTicketToRegistry(final Ticket ticket) throws Exception {
        addTicketToRegistry(ticket, null);
    }

    /**
     * Generate refresh token.
     *
     * @param responseHolder the response holder
     * @param accessToken    the related Access token
     * @return the refresh token
     * @throws Exception the exception
     */
    protected OAuth20RefreshToken generateRefreshToken(final AccessTokenRequestContext responseHolder,
                                                       final OAuth20AccessToken accessToken) throws Exception {
        LOGGER.debug("Creating refresh token for [{}]", responseHolder.getService());
        val refreshToken = this.refreshTokenFactory.create(responseHolder.getService(),
            responseHolder.getAuthentication(),
            responseHolder.getTicketGrantingTicket(),
            responseHolder.getScopes(),
            responseHolder.getRegisteredService().getClientId(),
            accessToken.getId(),
            responseHolder.getClaims(),
            responseHolder.getResponseType(),
            responseHolder.getGrantType());
        LOGGER.debug("Adding refresh token [{}] to the registry", refreshToken);
        addTicketToRegistry(refreshToken, responseHolder.getTicketGrantingTicket());
        if (responseHolder.isExpireOldRefreshToken()) {
            expireOldRefreshToken(responseHolder);
        }
        return refreshToken;
    }

    private OAuth20DeviceUserCode getDeviceUserCodeFromRegistry(final OAuth20DeviceToken deviceCodeTicket) {
        return FunctionUtils.doAndHandle(
                () -> ticketRegistry.getTicket(deviceCodeTicket.getUserCode(), OAuth20DeviceUserCode.class),
                throwable -> {
                    LOGGER.error("Provided user code [{}] is invalid or expired and cannot be found in the ticket registry",
                        deviceCodeTicket.getUserCode());
                    throw new InvalidOAuth20DeviceTokenException(deviceCodeTicket.getUserCode());
                })
            .get();
    }

    private OAuth20DeviceToken getDeviceTokenFromTicketRegistry(final String deviceCode) {
        return FunctionUtils.doAndHandle(
                () -> ticketRegistry.getTicket(deviceCode, OAuth20DeviceToken.class),
                throwable -> {
                    LoggingUtils.error(LOGGER, throwable);
                    throw new InvalidOAuth20DeviceTokenException(deviceCode);
                })
            .get();
    }

    private Pair<OAuth20DeviceToken, OAuth20DeviceUserCode> createDeviceTokensInTicketRegistry(
        final AccessTokenRequestContext holder) throws Exception {
        val deviceToken = deviceTokenFactory.createDeviceCode(holder.getService());
        LOGGER.debug("Created device code token [{}]", deviceToken.getId());

        val deviceUserCode = deviceUserCodeFactory.createDeviceUserCode(deviceToken);
        LOGGER.debug("Created device user code token [{}]", deviceUserCode.getId());

        addTicketToRegistry(deviceToken);
        LOGGER.debug("Added device token [{}] to registry", deviceToken);

        addTicketToRegistry(deviceUserCode);
        LOGGER.debug("Added device user token [{}] to registry", deviceUserCode);

        return Pair.of(deviceToken, deviceUserCode);
    }

    private void expireOldRefreshToken(final AccessTokenRequestContext responseHolder) throws Exception {
        val oldRefreshToken = responseHolder.getToken();
        LOGGER.debug("Expiring old refresh token [{}]", oldRefreshToken);
        oldRefreshToken.markTicketExpired();
        ticketRegistry.deleteTicket(oldRefreshToken);
    }
}
