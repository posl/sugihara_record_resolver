package org.apereo.cas.support.oauth.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;

/**
 * This is {@link DefaultRegisteredServiceOAuthCodeExpirationPolicy}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@ToString
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DefaultRegisteredServiceOAuthCodeExpirationPolicy implements RegisteredServiceOAuthCodeExpirationPolicy {
    @Serial
    private static final long serialVersionUID = 8435436756392637728L;

    private long numberOfUses;

    private String timeToLive;
}
