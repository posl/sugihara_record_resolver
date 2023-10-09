/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 13/dic/2011
 * Copyright 2017 by Sober Lemur S.a.s. di Vacondio Andrea (info@pdfsam.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pdfsam.i18n;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import org.pdfsam.eventstudio.annotation.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.pdfsam.eventstudio.StaticStudio.eventStudio;

/**
 * Context to deal with translations. It contains information about the mutable current locale and allow to translate strings to the current locale through static methods.
 *
 * @author Andrea Vacondio
 */
public final class I18nContext {

    private static final Logger LOG = LoggerFactory.getLogger(I18nContext.class);

    private final Set<Locale> supported = Set.of(new Locale("af"), new Locale("eu"), new Locale("bs"),
            new Locale("pt", "BR"), Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE, new Locale("co"),
            new Locale("hr"), new Locale("cs"), new Locale("da"), new Locale("nl"), Locale.UK, Locale.FRENCH,
            Locale.GERMAN, new Locale("he"), new Locale("hu"), new Locale("el"), Locale.JAPANESE, Locale.ITALIAN,
            new Locale("pl"), new Locale("pt"), new Locale("ro"), new Locale("ru"), new Locale("sk"), new Locale("sl"),
            new Locale("sr"), new Locale("sv"), new Locale("es"), new Locale("tr"), new Locale("uk"), new Locale("fi"),
            new Locale("ko"));

    private final ReplaySubject<Locale> locale = ReplaySubject.createWithSize(1);

    private Optional<ResourceBundle> bundle = empty();

    I18nContext() {
        eventStudio().addAnnotatedListeners(this);
        locale.filter(Objects::nonNull)
                .subscribe(this::loadBundle, e -> LOG.error("Unable to load translations bundle", e));
    }

    @EventListener
    public void setLocale(SetLocaleRequest e) {
        if (nonNull(e.languageTag()) && !e.languageTag().isBlank()) {
            LOG.trace("Setting locale to {}", e.languageTag());
            ofNullable(Locale.forLanguageTag(e.languageTag())).filter(supported::contains).ifPresent(locale::onNext);
        }
    }

    private void loadBundle(Locale l) {
        if (nonNull(l)) {
            Locale.setDefault(l);
            LOG.trace("Loading i18n bundle for {}", Locale.getDefault());
            this.bundle = ofNullable(ResourceBundle.getBundle("org.pdfsam.i18n.Messages", Locale.getDefault(),
                    I18nContext.class.getModule()));
            LOG.debug("Locale set to {}", Locale.getDefault());
        }
    }

    Locale getBestLocale() {
        if (supported.contains(Locale.getDefault())) {
            LOG.trace("Using best matching locale: {}", Locale.getDefault());
            return Locale.getDefault();
        }
        Locale onlyLanguage = new Locale(Locale.getDefault().getLanguage());
        if (supported.contains(onlyLanguage)) {
            LOG.trace("Using supported locale closest to default {}", onlyLanguage);
            return onlyLanguage;
        }
        LOG.trace("Using fallback locale");
        return Locale.ENGLISH;
    }

    /**
     * @return the default {@link I18nContext} instance
     */
    public static I18nContext i18n() {
        return I18nContextHolder.CONTEXT;
    }

    /**
     * @return an {@link Observable} {@link Locale} representing the current locale
     */
    public Observable<Locale> locale() {
        return this.locale.hide();

    }

    public String tr(String text) {
        initBundleIfRequired();
        return bundle.filter(r -> r.containsKey(text)).map(r -> r.getString(text)).orElse(text);
    }

    /**
     * @param text    text to be translated
     * @param replace replacements for the placeholders
     * @return the translated string where {0} and {1} (etc) placeholders are replaced by the replace[0], replace[1] etc
     */
    public String tr(String text, String... replace) {
        initBundleIfRequired();
        return MessageFormat.format(tr(text), (Object[]) replace);
    }

    private void initBundleIfRequired() {
        if (bundle.isEmpty()) {
            locale.onNext(getBestLocale());
        }
    }

    public Set<Locale> getSupported() {
        return supported;
    }

    /**
     * Lazy initialization holder class idiom (Joshua Bloch, Effective Java second edition, item 71).
     *
     * @author Andrea Vacondio
     */
    private static final class I18nContextHolder {

        private I18nContextHolder() {
            // hide constructor
        }

        static final I18nContext CONTEXT = new I18nContext();
    }
}
