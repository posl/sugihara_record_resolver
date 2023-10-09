package org.apereo.cas.support.sms;

import org.apereo.cas.configuration.model.support.sms.SmsModeProperties;
import org.apereo.cas.notifications.sms.SmsSender;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.HttpUtils;
import org.apereo.cas.util.LoggingUtils;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.HttpResponse;
import org.apereo.inspektr.common.web.ClientInfoHolder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * This is {@link SmsModeSmsSender}.
 *
 * @author Jérôme Rautureau
 * @since 6.5.0
 */
@Slf4j
public record SmsModeSmsSender(SmsModeProperties properties) implements SmsSender {
    private static final String SMSMODE_SENT_SMS_RESPONSE_CODE = "0";

    @Override
    public boolean send(final String from, final String to, final String message) {
        HttpResponse response = null;
        try {
            val parameters = new HashMap<String, String>();
            val holder = ClientInfoHolder.getClientInfo();
            if (holder != null) {
                parameters.put("clientIpAddress", holder.getClientIpAddress());
                parameters.put("serverIpAddress", holder.getServerIpAddress());
            }
            parameters.put("from", from);
            parameters.put(properties.getToAttribute(), to);
            parameters.put(properties.getMessageAttribute(), message);

            val headers = CollectionUtils.<String, String>wrap("Content-Type", MediaType.TEXT_PLAIN_VALUE);
            headers.putAll(properties.getHeaders());
            val exec = HttpUtils.HttpExecutionRequest.builder()
                .method(HttpMethod.GET)
                .url(properties.getUrl())
                .parameters(parameters)
                .headers(headers)
                .build();
            response = HttpUtils.execute(exec);
            val status = HttpStatus.valueOf(response.getCode());
            if (status.is2xxSuccessful()) {
                val entity = ((HttpEntityContainer) response).getEntity();
                val charset = entity.getContentEncoding() != null
                    ? Charset.forName(entity.getContentEncoding())
                    : StandardCharsets.ISO_8859_1;
                val resp = IOUtils.toString(entity.getContent(), charset);
                LOGGER.debug("Response from SmsMode: [{}]", resp);
                return StringUtils.startsWith(resp, SMSMODE_SENT_SMS_RESPONSE_CODE);
            }
        } catch (final Exception e) {
            LoggingUtils.error(LOGGER, e);
        } finally {
            HttpUtils.close(response);
        }
        return false;
    }

}
