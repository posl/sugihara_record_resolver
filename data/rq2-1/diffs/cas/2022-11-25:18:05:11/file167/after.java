package org.apereo.cas.authentication.attribute;

import org.apereo.cas.configuration.support.ExpressionLanguageCapable;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.EncodingUtils;
import org.apereo.cas.util.scripting.ExecutableCompiledGroovyScript;
import org.apereo.cas.util.scripting.ScriptingUtils;
import org.apereo.cas.util.spring.ApplicationContextProvider;
import org.apereo.cas.util.spring.SpringExpressionLanguageValueResolver;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.With;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apereo.services.persondir.util.CaseCanonicalizationMode;
import org.jooq.lambda.Unchecked;

import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This is {@link DefaultAttributeDefinition}.
 *
 * @author Misagh Moayyed
 * @author Travis Schmidt
 * @since 6.2.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@ToString
@EqualsAndHashCode(of = "key")
@Getter
@Setter
@AllArgsConstructor
@SuperBuilder
@NoArgsConstructor
@Slf4j
@With
public class DefaultAttributeDefinition implements AttributeDefinition {
    @Serial
    private static final long serialVersionUID = 6898745248727445565L;

    private String key;

    private String name;

    private boolean scoped;

    private boolean encrypted;

    private String attribute;

    private String patternFormat;

    @ExpressionLanguageCapable
    private String script;

    private String canonicalizationMode;

    private static List<Object> formatValuesWithScope(final String scope, final List<Object> currentValues) {
        return currentValues
            .stream()
            .map(v -> String.format("%s@%s", v, scope))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<Object> encryptValues(final List<Object> currentValues, final RegisteredService registeredService) {
        val publicKey = registeredService.getPublicKey();
        if (publicKey == null) {
            LOGGER.error("No public key is defined for service [{}]. No attributes will be released", registeredService);
            return new ArrayList<>(0);
        }
        val cipher = publicKey.toCipher();
        if (cipher == null) {
            LOGGER.error("Unable to initialize cipher given the public key algorithm [{}]", publicKey.getAlgorithm());
            return new ArrayList<>(0);
        }

        return currentValues
            .stream()
            .map(Unchecked.function(value -> {
                LOGGER.trace("Encrypting attribute value [{}]", value);
                val result = EncodingUtils.encodeBase64(cipher.doFinal(value.toString().getBytes(StandardCharsets.UTF_8)));
                LOGGER.trace("Encrypted attribute value [{}]", result);
                return result;
            }))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<Object> fetchAttributeValueFromExternalGroovyScript(final String attributeName,
                                                                            final List<Object> currentValues,
                                                                            final String file,
                                                                            final AttributeDefinitionResolutionContext context) {
        val result = ApplicationContextProvider.getScriptResourceCacheManager();
        if (result.isPresent()) {
            val cacheMgr = result.get();
            val script = cacheMgr.resolveScriptableResource(file, attributeName, file);
            if (script != null) {
                return fetchAttributeValueFromScript(script, attributeName, currentValues, context);
            }
        }
        LOGGER.warn("No groovy script cache manager is available to execute attribute mappings");
        return new ArrayList<>(0);
    }

    private static List<Object> fetchAttributeValueAsInlineGroovyScript(final String attributeName,
                                                                        final List<Object> currentValues,
                                                                        final String inlineGroovy,
                                                                        final AttributeDefinitionResolutionContext context) {
        val result = ApplicationContextProvider.getScriptResourceCacheManager();
        if (result.isPresent()) {
            val cacheMgr = result.get();
            val script = cacheMgr.resolveScriptableResource(inlineGroovy, attributeName, inlineGroovy);
            return fetchAttributeValueFromScript(script, attributeName, currentValues, context);
        }
        LOGGER.warn("No groovy script cache manager is available to execute attribute mappings");
        return new ArrayList<>(0);
    }

    private static List<Object> fetchAttributeValueFromScript(final ExecutableCompiledGroovyScript scriptToExec,
                                                              final String attributeKey,
                                                              final List<Object> currentValues,
                                                              final AttributeDefinitionResolutionContext context) {
        val args = CollectionUtils.<String, Object>wrap("attributeName", Objects.requireNonNull(attributeKey),
            "attributeValues", currentValues, "logger", LOGGER,
            "registeredService", context.getRegisteredService(),
            "attributes", context.getAttributes());
        scriptToExec.setBinding(args);
        return scriptToExec.execute(args.values().toArray(), List.class);
    }

    @Override
    public int compareTo(final AttributeDefinition o) {
        return new CompareToBuilder()
            .append(getKey(), o.getKey())
            .build();
    }

    @JsonIgnore
    @Override
    public List<Object> resolveAttributeValues(final AttributeDefinitionResolutionContext context) {
        List<Object> currentValues = new ArrayList<>(context.getAttributeValues());
        if (StringUtils.isNotBlank(getScript())) {
            currentValues = getScriptedAttributeValue(key, currentValues, context);
        }
        if (isScoped()) {
            currentValues = formatValuesWithScope(context.getScope(), currentValues);
        }
        if (StringUtils.isNotBlank(getPatternFormat())) {
            currentValues = formatValuesWithPattern(currentValues);
        }
        if (isEncrypted()) {
            currentValues = encryptValues(currentValues, context.getRegisteredService());
        }
        if (StringUtils.isNotBlank(this.canonicalizationMode)) {
            val mode = CaseCanonicalizationMode.valueOf(canonicalizationMode.toUpperCase());
            currentValues = currentValues
                .stream()
                .map(value -> mode.canonicalize(value.toString()))
                .collect(Collectors.toList());
        }
        LOGGER.trace("Resolved values [{}] for attribute definition [{}]", currentValues, this);
        return currentValues;
    }

    private List<Object> formatValuesWithPattern(final List<Object> currentValues) {
        return currentValues
            .stream()
            .map(v -> MessageFormat.format(getPatternFormat(), v))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @JsonIgnore
    private List<Object> getScriptedAttributeValue(final String attributeKey,
                                                   final List<Object> currentValues,
                                                   final AttributeDefinitionResolutionContext context) {
        LOGGER.trace("Locating attribute value via script for definition [{}]", this);
        val matcherInline = ScriptingUtils.getMatcherForInlineGroovyScript(getScript());

        if (matcherInline.find()) {
            return fetchAttributeValueAsInlineGroovyScript(attributeKey, currentValues, matcherInline.group(1), context);
        }

        val scriptDefinition = SpringExpressionLanguageValueResolver.getInstance().resolve(getScript());
        val matcherFile = ScriptingUtils.getMatcherForExternalGroovyScript(scriptDefinition);
        if (matcherFile.find()) {
            return fetchAttributeValueFromExternalGroovyScript(attributeKey, currentValues, matcherFile.group(), context);
        }

        return new ArrayList<>(0);
    }
}
