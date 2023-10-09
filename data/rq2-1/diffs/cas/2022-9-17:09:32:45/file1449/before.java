package org.apereo.cas.ticket.query;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.ticket.ExpirationPolicy;
import org.apereo.cas.ticket.ExpirationPolicyBuilder;
import org.apereo.cas.ticket.expiration.HardTimeoutExpirationPolicy;
import org.apereo.cas.ticket.expiration.NeverExpiresExpirationPolicy;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;

/**
 * This is {@link SamlAttributeQueryTicketExpirationPolicyBuilder}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@RequiredArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@ToString
@Getter
public class SamlAttributeQueryTicketExpirationPolicyBuilder implements ExpirationPolicyBuilder<SamlAttributeQueryTicket> {
    private static final long serialVersionUID = -3597980180617072826L;

    /**
     * The Cas properties.
     */
    protected final CasConfigurationProperties casProperties;

    @Override
    public ExpirationPolicy buildTicketExpirationPolicy() {
        return toTicketExpirationPolicy();
    }

    /**
     * To ticket expiration policy.
     *
     * @return the expiration policy
     */
    public ExpirationPolicy toTicketExpirationPolicy() {
        val timeToKillInSeconds = casProperties.getAuthn().getSamlIdp()
            .getTicket().getAttributeQuery().getTimeToKillInSeconds();
        return timeToKillInSeconds <= 0
            ? new NeverExpiresExpirationPolicy()
            : new HardTimeoutExpirationPolicy(timeToKillInSeconds);
    }
}

