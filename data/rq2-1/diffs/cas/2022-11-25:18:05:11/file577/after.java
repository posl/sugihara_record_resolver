package org.apereo.cas.web.view;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import javax.annotation.Nonnull;

import java.util.Locale;
import java.util.stream.IntStream;

/**
 * An extension of the {@link ReloadableResourceBundleMessageSource} whose sole concern
 * is to print a WARN message in cases where a language key is not found in the active and
 * default bundles.
 *
 * <p>Note: By default, if a key not found in a localized bundle, Spring will auto-fallback
 * to the default bundle that is {@code messages.properties}. However, if the key is also
 * not found in the default bundle, and {@link #setUseCodeAsDefaultMessage(boolean)}
 * is set to true, then only the requested code itself will be used as the message to display.
 * In this case, the class will issue a WARN message instructing the caller that the bundle
 * needs further attention. If {@link #setUseCodeAsDefaultMessage(boolean)} is set to false,
 * only then a {@code null} value will be returned, which subsequently causes an instance
 * of {@link org.springframework.context.NoSuchMessageException} to be thrown.
 *
 * @author Misagh Moayyed
 * @since 4.0.0
 */
@Slf4j
public class CasReloadableMessageBundle extends ReloadableResourceBundleMessageSource {

    private String[] basenames;

    @Override
    public void setBasenames(
        @Nonnull final String... basenames) {
        this.basenames = basenames;
        super.setBasenames(basenames);
    }

    @Override
    protected String getMessageInternal(final String code, final Object[] args, final Locale locale) {
        if (!locale.equals(Locale.ENGLISH)) {
            val foundCode = IntStream.range(0, this.basenames.length)
                .filter(i -> {
                    val filename = this.basenames[i] + '_' + locale;
                    LOGGER.trace("Examining bundle [{}] for the key [{}]", filename, code);
                    val holder = this.getProperties(filename);
                    return holder != null && holder.getProperties() != null && holder.getProperty(code) != null;
                })
                .findFirst()
                .isPresent();
            if (!foundCode) {
                LOGGER.trace("The key [{}] cannot be found in the bundle for the locale [{}]", code, locale);
            }
        }
        return super.getMessageInternal(code, args, locale);
    }

    @Override
    protected String getDefaultMessage(
        @Nonnull final String code) {
        val messageToReturn = super.getDefaultMessage(code);
        if (StringUtils.isNotBlank(messageToReturn) && messageToReturn.equals(code)) {
            LOGGER.trace("The code [{}] cannot be found in the default language bundle and will be used as the message itself.", code);
        }
        return messageToReturn;
    }

}
