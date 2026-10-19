package com.nsdev.lettergenerator.model;

import com.nsdev.lettergenerator.internal.util.ImmutableLists;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import com.nsdev.lettergenerator.model.layout.TextDirection;
import java.util.List;

/**
 * Optional language, formatting, metadata and explicit font fallback settings. No system-font
 * scanning or JVM default locale/time zone participates.
 *
 * @param locale country-aware formatting, null selects stable defaults
 * @param metadata optional PDF metadata
 * @param direction optional explicit direction; null retains ordinary left-to-right behavior
 * @param fallbackFonts ordered registered family names, null becomes empty
 * @param hyphenate whether registered language patterns should be used, null means false
 */
public record DocumentSettings(
        LocaleSettings locale,
        DocumentMetadata metadata,
        TextDirection direction,
        List<String> fallbackFonts,
        Boolean hyphenate) {
    /**
     * Snapshots options and supplies deterministic formatting defaults.
     *
     * @param locale optional formatting settings
     * @param metadata optional PDF metadata
     * @param direction optional writing direction
     * @param fallbackFonts optional ordered fallback families
     * @param hyphenate optional dictionary-based hyphenation
     */
    public DocumentSettings {
        locale = locale == null ? LocaleSettings.defaults() : locale;
        fallbackFonts =
                fallbackFonts == null ? List.of() : ImmutableLists.copyNullable(fallbackFonts);
        hyphenate = hyphenate == null ? false : hyphenate;
    }

    /**
     * @return defaults that preserve ordinary letter output
     */
    public static DocumentSettings defaults() {
        return new DocumentSettings(null, null, null, null, null);
    }

    /**
     * Returns a copy with country-aware formatting replaced.
     *
     * @param value new formatting settings
     * @return immutable settings
     */
    public DocumentSettings withLocale(LocaleSettings value) {
        return new DocumentSettings(value, metadata, direction, fallbackFonts, hyphenate);
    }
}
