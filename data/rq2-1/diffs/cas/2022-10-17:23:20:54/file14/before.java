package org.apereo.cas.configuration.model.support.syncope;

import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.configuration.model.core.authentication.AuthenticationHandlerStates;
import org.apereo.cas.configuration.model.core.authentication.PasswordEncoderProperties;
import org.apereo.cas.configuration.model.core.authentication.PrincipalTransformationProperties;
import org.apereo.cas.configuration.support.RequiresModule;

import com.fasterxml.jackson.annotation.JsonFilter;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.Serial;

/**
 * This is {@link SyncopeAuthenticationProperties}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@RequiresModule(name = "cas-server-support-syncope-authentication")
@Getter
@Setter
@Accessors(chain = true)
@JsonFilter("SyncopeAuthenticationProperties")
public class SyncopeAuthenticationProperties extends AbstractSyncopeProperties implements CasFeatureModule {

    @Serial
    private static final long serialVersionUID = -2446926316502297496L;

    /**
     * Define the scope and state of this authentication handler
     * and the lifecycle in which it can be invoked or activated.
     */
    private AuthenticationHandlerStates state = AuthenticationHandlerStates.ACTIVE;

    /**
     * Name of the authentication handler.
     */
    private String name;

    /**
     * Password encoder settings for the authentication handler.
     */
    @NestedConfigurationProperty
    private PasswordEncoderProperties passwordEncoder = new PasswordEncoderProperties();

    /**
     * A number of authentication handlers are allowed to determine whether they can operate on the provided credential
     * and as such lend themselves to be tried and tested during the authentication handler selection phase.
     * The credential criteria may be one of the following options:<ul>
     * <li>1) A regular expression pattern that is tested against the credential identifier.</li>
     * <li>2) A fully qualified class name of your own design that implements {@code Predicate}.</li>
     * <li>3) Path to an external Groovy script that implements the same interface.</li>
     * </ul>
     */
    private String credentialCriteria;

    /**
     * This is principal transformation properties.
     */
    @NestedConfigurationProperty
    private PrincipalTransformationProperties principalTransformation = new PrincipalTransformationProperties();

}
