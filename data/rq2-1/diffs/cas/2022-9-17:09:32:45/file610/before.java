package org.apereo.cas.support.events.authentication;

import org.apereo.cas.authentication.Credential;
import org.apereo.cas.support.events.AbstractCasEvent;

import lombok.Getter;
import lombok.ToString;


/**
 * This is {@link CasAuthenticationTransactionStartedEvent}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@ToString(callSuper = true)
@Getter
public class CasAuthenticationTransactionStartedEvent extends AbstractCasEvent {
    private static final long serialVersionUID = -1862937393590213811L;

    private final Credential credential;

    public CasAuthenticationTransactionStartedEvent(final Object source, final Credential c) {
        super(source);
        this.credential = c;
    }
}
