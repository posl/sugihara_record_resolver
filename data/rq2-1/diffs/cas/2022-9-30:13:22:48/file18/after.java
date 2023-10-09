package org.apereo.cas.authentication.attribute;

import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.util.scripting.GroovyScriptResourceCacheManager;
import org.apereo.cas.util.scripting.ScriptResourceCacheManager;
import org.apereo.cas.util.spring.ApplicationContextProvider;

import lombok.val;
import org.apereo.services.persondir.util.CaseCanonicalizationMode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link DefaultAttributeDefinitionTests}.
 *
 * @author Misagh Moayyed
 * @since 6.3.0
 */
@Tag("Authentication")
public class DefaultAttributeDefinitionTests {

    @Test
    public void verifyCaseCanonicalizationMode() {
        val applicationContext = new StaticApplicationContext();
        applicationContext.registerSingleton(ScriptResourceCacheManager.BEAN_NAME, GroovyScriptResourceCacheManager.class);
        applicationContext.refresh();
        ApplicationContextProvider.holdApplicationContext(applicationContext);

        val defn = DefaultAttributeDefinition.builder()
            .key("computedAttribute")
            .canonicalizationMode(CaseCanonicalizationMode.UPPER.name())
            .script("groovy { return ['value1', 'value2'] }")
            .build();

        val context = getAttributeDefinitionResolutionContext();

        val values = defn.resolveAttributeValues(context);
        assertTrue(values.contains("VALUE1"));
        assertTrue(values.contains("VALUE2"));
    }

    @Test
    public void verifyNoCacheEmbeddedScriptOperation() {
        val applicationContext = new StaticApplicationContext();
        applicationContext.refresh();
        ApplicationContextProvider.holdApplicationContext(applicationContext);

        val defn = DefaultAttributeDefinition.builder()
            .key("computedAttribute")
            .script("groovy { return ['hello world'] }")
            .build();
        val context = getAttributeDefinitionResolutionContext();
        val values = defn.resolveAttributeValues(context);
        assertTrue(values.isEmpty());
    }

    @Test
    public void verifyBadScript() {
        val applicationContext = new StaticApplicationContext();
        applicationContext.refresh();
        ApplicationContextProvider.holdApplicationContext(applicationContext);

        val defn = DefaultAttributeDefinition.builder()
            .key("computedAttribute")
            .script("badformat ()")
            .build();
        val context = getAttributeDefinitionResolutionContext();
        val values = defn.resolveAttributeValues(context);
        assertTrue(values.isEmpty());
    }

    @Test
    public void verifyCachedEmbeddedScriptOperation() {
        val applicationContext = new StaticApplicationContext();
        applicationContext.registerSingleton(ScriptResourceCacheManager.BEAN_NAME, GroovyScriptResourceCacheManager.class);
        applicationContext.refresh();
        ApplicationContextProvider.holdApplicationContext(applicationContext);

        val defn = DefaultAttributeDefinition.builder()
            .key("computedAttribute")
            .script("groovy { return ['hello world'] }")
            .build();
        val context = getAttributeDefinitionResolutionContext();
        var values = defn.resolveAttributeValues(context);
        assertFalse(values.isEmpty());
        values = defn.resolveAttributeValues(context);
        assertFalse(values.isEmpty());
    }

    @Test
    public void verifyNoCachedExternalScriptOperation() {
        val applicationContext = new StaticApplicationContext();
        applicationContext.refresh();
        ApplicationContextProvider.holdApplicationContext(applicationContext);

        val defn = DefaultAttributeDefinition.builder()
            .key("computedAttribute")
            .script("classpath:ComputedAttributeDefinition.groovy")
            .build();
        val context = getAttributeDefinitionResolutionContext();
        val values = defn.resolveAttributeValues(context);
        assertTrue(values.isEmpty());
    }

    @Test
    public void verifyCachedExternalScriptOperation() {
        val applicationContext = new StaticApplicationContext();
        applicationContext.registerSingleton(ScriptResourceCacheManager.BEAN_NAME, GroovyScriptResourceCacheManager.class);
        applicationContext.refresh();
        ApplicationContextProvider.holdApplicationContext(applicationContext);

        val defn = DefaultAttributeDefinition.builder()
            .key("computedAttribute")
            .script("classpath:ComputedAttributeDefinition.groovy")
            .build();
        val context = getAttributeDefinitionResolutionContext();
        var values = defn.resolveAttributeValues(context);
        assertFalse(values.isEmpty());
        values = defn.resolveAttributeValues(context);
        assertFalse(values.isEmpty());
    }

    @Test
    public void verifyBadExternalScriptOperation() {
        val applicationContext = new StaticApplicationContext();
        applicationContext.registerSingleton(ScriptResourceCacheManager.BEAN_NAME, GroovyScriptResourceCacheManager.class);
        applicationContext.refresh();
        ApplicationContextProvider.holdApplicationContext(applicationContext);

        val defn = DefaultAttributeDefinition.builder()
            .key("computedAttribute")
            .script("classpath:BadScript.groovy")
            .build();
        val context = getAttributeDefinitionResolutionContext();
        val values = defn.resolveAttributeValues(context);
        assertTrue(values.isEmpty());
    }

    @Test
    public void verifyBadEmbeddedScriptOperation() {
        val applicationContext = new StaticApplicationContext();
        applicationContext.registerSingleton(ScriptResourceCacheManager.BEAN_NAME, GroovyScriptResourceCacheManager.class);
        applicationContext.refresh();
        ApplicationContextProvider.holdApplicationContext(applicationContext);

        val defn = DefaultAttributeDefinition.builder()
            .key("computedAttribute")
            .script("groovy {xyz}")
            .build();
        val context = getAttributeDefinitionResolutionContext();
        val values = defn.resolveAttributeValues(context);
        assertNull(values);
    }

    private static AttributeDefinitionResolutionContext getAttributeDefinitionResolutionContext() {
        return AttributeDefinitionResolutionContext.builder()
            .attributeValues(List.of("v1", "v2"))
            .scope("example.org")
            .principal(CoreAuthenticationTestUtils.getPrincipal())
            .registeredService(CoreAuthenticationTestUtils.getRegisteredService())
            .service(CoreAuthenticationTestUtils.getService())
            .build();
    }
}
