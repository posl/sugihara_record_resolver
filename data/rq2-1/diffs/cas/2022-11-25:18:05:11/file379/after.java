package org.apereo.cas.services;

import org.apereo.cas.configuration.support.ExpressionLanguageCapable;
import org.apereo.cas.util.ResourceUtils;
import org.apereo.cas.util.function.FunctionUtils;
import org.apereo.cas.util.scripting.ScriptingUtils;
import org.apereo.cas.util.spring.SpringExpressionLanguageValueResolver;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

import jakarta.persistence.Transient;

import java.io.Serial;
import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * This is {@link GroovyRegisteredServiceAccessStrategy}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@EqualsAndHashCode(of = {"order", "groovyScript"}, callSuper = true)
public class GroovyRegisteredServiceAccessStrategy extends BaseRegisteredServiceAccessStrategy {

    @Serial
    private static final long serialVersionUID = -2407494148882123062L;

    /**
     * The sorting/execution order of this strategy.
     */
    private int order;

    @ExpressionLanguageCapable
    private String groovyScript;

    @JsonIgnore
    @Transient
    @org.springframework.data.annotation.Transient
    private transient RegisteredServiceAccessStrategy groovyStrategyInstance;

    @Override
    @JsonIgnore
    public boolean isServiceAccessAllowed() {
        buildGroovyAccessStrategyInstanceIfNeeded();
        return this.groovyStrategyInstance.isServiceAccessAllowed();
    }

    @Override
    @JsonIgnore
    public boolean isServiceAccessAllowedForSso() {
        buildGroovyAccessStrategyInstanceIfNeeded();
        return this.groovyStrategyInstance.isServiceAccessAllowedForSso();
    }

    @Override
    @JsonIgnore
    public boolean doPrincipalAttributesAllowServiceAccess(final RegisteredServiceAccessStrategyRequest request) {
        buildGroovyAccessStrategyInstanceIfNeeded();
        return this.groovyStrategyInstance.doPrincipalAttributesAllowServiceAccess(request);
    }

    @JsonIgnore
    @Override
    public URI getUnauthorizedRedirectUrl() {
        buildGroovyAccessStrategyInstanceIfNeeded();
        return this.groovyStrategyInstance.getUnauthorizedRedirectUrl();
    }

    @Override
    @JsonIgnore
    public RegisteredServiceDelegatedAuthenticationPolicy getDelegatedAuthenticationPolicy() {
        buildGroovyAccessStrategyInstanceIfNeeded();
        return this.groovyStrategyInstance.getDelegatedAuthenticationPolicy();
    }

    @Override
    @JsonIgnore
    public Map<String, Set<String>> getRequiredAttributes() {
        return this.groovyStrategyInstance.getRequiredAttributes();
    }

    private void buildGroovyAccessStrategyInstanceIfNeeded() {
        if (this.groovyStrategyInstance == null) {
            val groovyResource = FunctionUtils.doUnchecked(() -> {
                val location = SpringExpressionLanguageValueResolver.getInstance().resolve(this.groovyScript);
                return ResourceUtils.getResourceFrom(location);
            });
            this.groovyStrategyInstance = ScriptingUtils.getObjectInstanceFromGroovyResource(groovyResource, RegisteredServiceAccessStrategy.class);
        }
    }
}
