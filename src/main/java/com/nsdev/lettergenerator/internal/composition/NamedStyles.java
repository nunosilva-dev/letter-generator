package com.nsdev.lettergenerator.internal.composition;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.theme.LetterTheme;
import com.nsdev.lettergenerator.theme.StyleRole;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Resolves document-local named styles without changing registered theme values. */
final class NamedStyles {
    /** Nullable source of all definitions for this document. */
    private final LetterTheme theme;

    /** Fully resolved named values, shared only inside this preparation. */
    private final Map<String, Style> resolved = new LinkedHashMap<>();

    /** Ordered active references used to diagnose cycles. */
    private final Set<String> active = new LinkedHashSet<>();

    /** Whether any source style needed its retained lower-priority layers materialized. */
    private boolean resolvedLayers;

    /**
     * Creates a resolver with no global registration or mutable shared state.
     *
     * @param theme nullable per-document theme
     */
    NamedStyles(LetterTheme theme) {
        this.theme = theme;
    }

    /** Validates every registered name and reference, including unused definitions. */
    void validate() {
        if (theme == null) {
            return;
        }
        for (var entry : theme.namedStyles().entrySet()) {
            requireName(entry.getKey(), "theme.namedStyles");
            named(entry.getKey(), "theme.namedStyles[" + entry.getKey() + "]");
        }
        for (var entry : theme.roles().entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new InvalidLetterException("theme.roles", "Role and style are required");
            }
            resolve(entry.getValue(), "theme.roles[" + entry.getKey() + "]");
        }
    }

    /**
     * Applies an optional named base below local properties, then clears the reference.
     *
     * @param style nullable local style
     * @param path input path for diagnostics
     * @return immutable resolved local overrides, or null
     */
    Style resolve(Style style, String path) {
        if (style == null) {
            return style;
        }
        if (style.baseStyle() == null && style.styleName() == null) {
            return style;
        }
        Style result = Style.plain();
        if (style.baseStyle() != null) {
            resolvedLayers = true;
            result = resolve(style.baseStyle(), path + ".baseStyle");
        }
        if (style.styleName() != null) {
            requireName(style.styleName(), path + ".styleName");
            result = result.overlay(named(style.styleName(), path + ".styleName"));
        }
        return result.overlay(withoutName(style));
    }

    /**
     * @return whether preparation must return rebuilt values even without a theme or components
     */
    boolean hasResolvedLayers() {
        return resolvedLayers;
    }

    /**
     * Retrieves a resolved semantic-role default.
     *
     * @param role semantic location
     * @return resolved defaults, or an empty set of overrides
     */
    Style role(StyleRole role) {
        Style value = theme == null ? null : theme.roles().get(role);
        return value == null ? Style.plain() : resolve(value, "theme.roles[" + role + "]");
    }

    /**
     * Resolves and memoizes a registered name with ordered cycle detection.
     *
     * @param name requested registration
     * @param path referring input path
     * @return fully resolved style without a named reference
     */
    private Style named(String name, String path) {
        if (resolved.containsKey(name)) {
            return resolved.get(name);
        }
        if (theme == null || !theme.namedStyles().containsKey(name)) {
            throw new InvalidLetterException(path, "Unknown named style '" + name + "'");
        }
        Style value = theme.namedStyles().get(name);
        if (value == null) {
            throw new InvalidLetterException(
                    "theme.namedStyles[" + name + "]", "Style is required");
        }
        if (!active.add(name)) {
            throw new InvalidLetterException(
                    path, "Named style cycle: " + String.join(" -> ", active) + " -> " + name);
        }
        try {
            Style result = resolve(value, "theme.namedStyles[" + name + "]");
            resolved.put(name, result);
            return result;
        } finally {
            active.remove(name);
        }
    }

    /**
     * Checks a registration or reference name before map lookup.
     *
     * @param name nullable name
     * @param path input location
     */
    static void requireName(String name, String path) {
        if (name == null || name.isBlank()) {
            throw new InvalidLetterException(path, "A nonblank name is required");
        }
    }

    /**
     * Clears only composition metadata while retaining explicit false, zero and pagination values.
     *
     * @param value local style containing a named reference
     * @return immutable ordinary style
     */
    private static Style withoutName(Style value) {
        return new Style(
                value.fontFamily(),
                value.fontSizePt(),
                value.color(),
                value.bold(),
                value.italic(),
                value.underline(),
                value.textAlign(),
                value.lineHeight(),
                value.spaceBeforeMm(),
                value.spaceAfterMm(),
                value.keepTogether(),
                value.keepWithNext(),
                value.widows(),
                value.orphans(),
                null);
    }
}
