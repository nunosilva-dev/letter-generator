package com.nsdev.lettergenerator.theme;

import com.nsdev.lettergenerator.model.style.Style;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable per-document role defaults and named styles. Theme values never modify global state.
 * Each nullable style property inherits independently; explicit document, section and content
 * overrides take precedence over role defaults. Named styles may reference another named style
 * through {@link Style#styleName()}; generation rejects unknown names and cycles.
 *
 * @param roles role defaults; null means no role overrides
 * @param namedStyles reusable named overrides; null means no named styles
 * @param tableAppearance optional table geometry/paint defaults
 * @param stationery optional repeating region defaults
 */
public record LetterTheme(
        Map<StyleRole, Style> roles,
        Map<String, Style> namedStyles,
        TableAppearance tableAppearance,
        Stationery stationery) {
    /**
     * Snapshots both maps, preserving registration order and invalid entries for validation.
     *
     * @param roles nullable role map
     * @param namedStyles nullable named-style map
     * @param tableAppearance optional table appearance
     * @param stationery optional fixed regions
     */
    public LetterTheme {
        roles = roles == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(roles));
        namedStyles =
                namedStyles == null
                        ? Map.of()
                        : Collections.unmodifiableMap(new LinkedHashMap<>(namedStyles));
    }

    /**
     * Preserves themes that configure only typography.
     *
     * @param roles role defaults
     * @param namedStyles named registrations
     */
    public LetterTheme(Map<StyleRole, Style> roles, Map<String, Style> namedStyles) {
        this(roles, namedStyles, null, null);
    }

    /**
     * @return a new independent builder with no role or named defaults
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Mutable accumulator whose built themes own snapshots of both registrations. */
    public static final class Builder {
        /** Role registrations retained in insertion order. */
        private final Map<StyleRole, Style> roles = new LinkedHashMap<>();

        /** Named registrations retained in insertion order. */
        private final Map<String, Style> namedStyles = new LinkedHashMap<>();

        /** Optional table geometry defaults. */
        private TableAppearance tableAppearance;

        /** Optional inherited fixed regions. */
        private Stationery stationery;

        /** Creates an empty builder through {@link LetterTheme#builder()}. */
        private Builder() {}

        /**
         * Registers or replaces a semantic role default.
         *
         * @param role semantic content role
         * @param style nullable style, checked during generation
         * @return this builder
         */
        public Builder role(StyleRole role, Style style) {
            roles.put(role, style);
            return this;
        }

        /**
         * Registers or replaces a named style.
         *
         * @param name nonblank name checked during generation
         * @param style style overrides checked during generation
         * @return this builder
         */
        public Builder named(String name, Style style) {
            namedStyles.put(name, style);
            return this;
        }

        /**
         * Sets ordinary-content defaults.
         *
         * @param style role overrides
         * @return this builder
         */
        public Builder body(Style style) {
            return role(StyleRole.BODY, style);
        }

        /**
         * Sets heading defaults.
         *
         * @param style role overrides
         * @return this builder
         */
        public Builder heading(Style style) {
            return role(StyleRole.HEADING, style);
        }

        /**
         * Sets table defaults.
         *
         * @param style role overrides
         * @return this builder
         */
        public Builder table(Style style) {
            return role(StyleRole.TABLE, style);
        }

        /**
         * Sets repeating table-header cell defaults.
         *
         * @param style role overrides
         * @return this builder
         */
        public Builder tableHeader(Style style) {
            return role(StyleRole.TABLE_HEADER, style);
        }

        /**
         * Sets table-body cell defaults.
         *
         * @param style role overrides
         * @return this builder
         */
        public Builder tableCell(Style style) {
            return role(StyleRole.TABLE_CELL, style);
        }

        /**
         * Sets reserved top-region defaults.
         *
         * @param style role overrides
         * @return this builder
         */
        public Builder header(Style style) {
            return role(StyleRole.HEADER, style);
        }

        /**
         * Sets reserved bottom-region defaults.
         *
         * @param style role overrides
         * @return this builder
         */
        public Builder footer(Style style) {
            return role(StyleRole.FOOTER, style);
        }

        /**
         * Sets visual-signature defaults.
         *
         * @param style role overrides
         * @return this builder
         */
        public Builder signature(Style style) {
            return role(StyleRole.SIGNATURE, style);
        }

        /**
         * Sets table paint, padding and row defaults.
         *
         * @param value optional table defaults
         * @return this builder
         */
        public Builder tableAppearance(TableAppearance value) {
            tableAppearance = value;
            return this;
        }

        /**
         * Sets reusable header/footer/logo content and geometry.
         *
         * @param value optional stationery
         * @return this builder
         */
        public Builder stationery(Stationery value) {
            stationery = value;
            return this;
        }

        /**
         * @return an immutable snapshot unaffected by subsequent builder calls
         */
        public LetterTheme build() {
            return new LetterTheme(roles, namedStyles, tableAppearance, stationery);
        }
    }
}
