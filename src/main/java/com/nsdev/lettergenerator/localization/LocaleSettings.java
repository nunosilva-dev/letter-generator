package com.nsdev.lettergenerator.localization;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Currency;
import java.util.Locale;

/**
 * Deterministic per-document formatting, independent of JVM defaults.
 *
 * @param languageTag BCP 47 locale; null defaults to en-GB
 * @param datePattern optional explicit date pattern
 * @param dateStyle localized date style; null means LONG
 * @param currencyCode optional ISO currency code; otherwise derived from the locale country
 * @param zoneId explicit time zone; null means UTC
 */
public record LocaleSettings(
        String languageTag,
        String datePattern,
        FormatStyle dateStyle,
        String currencyCode,
        String zoneId) {
    /** Deterministic default formatting language and country. */
    private static final String DEFAULT_LANGUAGE_TAG = "en-GB";

    /** Stable default timestamp conversion zone. */
    private static final String DEFAULT_ZONE_ID = "UTC";

    /**
     * Validates and normalizes formatting choices immediately.
     *
     * @param languageTag explicit language tag or stable default
     * @param datePattern optional exact pattern
     * @param dateStyle optional localized style
     * @param currencyCode optional ISO currency
     * @param zoneId optional explicit zone
     */
    public LocaleSettings {
        languageTag = languageTag == null ? DEFAULT_LANGUAGE_TAG : languageTag;
        Locale language;
        try {
            language = new Locale.Builder().setLanguageTag(languageTag).build();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid language tag: " + languageTag, exception);
        }
        if (language.getLanguage().isEmpty()) {
            throw new IllegalArgumentException("Language tag must name a language");
        }
        languageTag = language.toLanguageTag();
        dateStyle = dateStyle == null ? FormatStyle.LONG : dateStyle;
        zoneId = zoneId == null ? DEFAULT_ZONE_ID : ZoneId.of(zoneId).getId();
        if (currencyCode != null) {
            currencyCode = Currency.getInstance(currencyCode).getCurrencyCode();
        }
        if (datePattern != null) {
            if (datePattern.isBlank()) {
                throw new IllegalArgumentException("Date pattern must not be blank");
            }
            DateTimeFormatter.ofPattern(datePattern, language);
        }
    }

    /**
     * Uses localized long dates, country currency and UTC.
     *
     * @param languageTag explicit locale language tag
     */
    public LocaleSettings(String languageTag) {
        this(languageTag, null, null, null, null);
    }

    /**
     * @return stable British English formatting
     */
    public static LocaleSettings defaults() {
        return new LocaleSettings(DEFAULT_LANGUAGE_TAG);
    }

    /**
     * @return explicitly configured Java locale
     */
    public Locale locale() {
        return Locale.forLanguageTag(languageTag);
    }
}
