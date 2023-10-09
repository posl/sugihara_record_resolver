package org.apereo.cas.util.scripting;

import groovy.lang.Script;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;

/**
 * This is {@link GroovyShellScript}.
 *
 * @author Misagh Moayyed
 * @since 6.0.0
 */
@Getter
public class GroovyShellScript implements ExecutableCompiledGroovyScript {
    private final transient Script groovyScript;
    private final String script;

    public GroovyShellScript(final String script) {
        this.script = script;
        this.groovyScript = ScriptingUtils.parseGroovyShellScript(script);
    }

    @Override
    public <T> T execute(final Object[] args, final Class<T> clazz) {
        return execute(args, clazz, true);
    }

    @Override
    public void execute(final Object[] args) {
        execute(args, Void.class, true);
    }

    @Override
    public <T> T execute(final Object[] args, final Class<T> clazz, final boolean failOnError) {
        if (this.groovyScript != null) {
            return ScriptingUtils.executeGroovyShellScript(this.groovyScript, clazz);
        }
        return null;
    }

    @Override
    public <T> T execute(final String methodName, final Class<T> clazz, final Object... args) {
        return execute(args, clazz);
    }

    @Override
    public void setBinding(final Map<String, Object> variables) {
        if (variables != null && !variables.isEmpty()) {
            val binding = this.groovyScript.getBinding();
            variables.forEach(binding::setVariable);
        }
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("script", script)
            .toString();
    }
}
