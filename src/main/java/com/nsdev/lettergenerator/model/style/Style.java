package com.nsdev.lettergenerator.model.style;

/**
 * Immutable set of nullable typography and spacing overrides. Null means inherit that property; an
 * explicit false disables an inherited boolean flag. During rendering, built-in text defaults are
 * overlaid by document, section, block, optional table-cell, and text-run styles in that order.
 * Fixed regions and signatures have no section style.
 *
 * <p>Built-in text defaults are Helvetica, 11 pt (14 pt and bold for headings), black, left
 * alignment, a 1.2 line-height multiplier, and zero space before/after. The other flags are false.
 * {@link #plain()} has no overrides and therefore preserves those or any inherited values; it does
 * not reset text to the built-in defaults.
 *
 * <p>Construction and builder methods do not validate values. Generation requires registered or
 * built-in font families and available bold/italic variants, positive finite font sizes and
 * line-height multipliers, finite nonnegative spacing, and colors in {@code #RRGGBB} form.
 *
 * @param fontFamily built-in or registered family name, or null to inherit
 * @param fontSizePt font size in points (1/72 inch), or null to inherit
 * @param color six-digit hexadecimal RGB text color, or null to inherit
 * @param bold bold flag, or null to inherit
 * @param italic italic flag, or null to inherit
 * @param underline underline flag, or null to inherit
 * @param textAlign horizontal paragraph alignment, or null to inherit
 * @param lineHeight dimensionless multiplier of font size, or null to inherit
 * @param spaceBeforeMm spacing before the block in millimeters, or null to inherit
 * @param spaceAfterMm spacing after the block in millimeters, or null to inherit
 */
public record Style(
        String fontFamily,
        Double fontSizePt,
        String color,
        Boolean bold,
        Boolean italic,
        Boolean underline,
        TextAlign textAlign,
        Double lineHeight,
        Double spaceBeforeMm,
        Double spaceAfterMm) {
    /**
     * Creates an empty set of overrides. Every property is null and inherits its enclosing style or
     * the renderer's defaults.
     *
     * @return a style that leaves all inherited properties unchanged
     */
    public static Style plain() {
        return new Style(null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Starts a mutable builder with every override omitted.
     *
     * @return a new independent style builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Combines these overrides with another style property by property. Each nonnull value in the
     * argument wins; its null values retain this style's values. Neither style is mutated.
     *
     * @param override higher-priority style, or null to leave this style unchanged
     * @return combined overrides, or this instance when override is null
     */
    public Style overlay(Style override) {
        if (override == null) {
            return this;
        }
        return new Style(
                inherited(override.fontFamily, fontFamily),
                        inherited(override.fontSizePt, fontSizePt),
                inherited(override.color, color), inherited(override.bold, bold),
                inherited(override.italic, italic), inherited(override.underline, underline),
                inherited(override.textAlign, textAlign),
                        inherited(override.lineHeight, lineHeight),
                inherited(override.spaceBeforeMm, spaceBeforeMm),
                        inherited(override.spaceAfterMm, spaceAfterMm));
    }

    /**
     * Chooses an explicitly supplied property over an inherited value.
     *
     * @param <T> property value type
     * @param value nullable override
     * @param fallback inherited value, possibly null
     * @return value when nonnull, otherwise fallback
     */
    private static <T> T inherited(T value, T fallback) {
        return value == null ? fallback : value;
    }

    /**
     * Mutable accumulator for an immutable {@link Style}. Omitted properties remain null, so they
     * inherit rather than resetting enclosing defaults. Setters store values without validation;
     * {@link #build()} takes a snapshot that later setter calls do not change.
     */
    public static final class Builder {
        /** Nullable family override; null retains the enclosing font family. */
        private String fontFamily;

        /** Nullable size override in points; null retains the enclosing size. */
        private Double fontSizePt;

        /** Nullable RGB text-color override; null retains the enclosing color. */
        private String color;

        /** Nullable weight override; false explicitly cancels inherited bold. */
        private Boolean bold;

        /** Nullable slant override; false explicitly cancels inherited italic. */
        private Boolean italic;

        /** Nullable underline override; false explicitly cancels inherited underlining. */
        private Boolean underline;

        /** Nullable paragraph alignment override. */
        private TextAlign textAlign;

        /** Nullable dimensionless line-height multiplier override. */
        private Double lineHeight;

        /** Nullable spacing before a block, in millimeters. */
        private Double spaceBeforeMm;

        /** Nullable spacing after a block, in millimeters. */
        private Double spaceAfterMm;

        /**
         * Creates an empty override accumulator; obtain instances through {@link Style#builder()}.
         */
        private Builder() {}

        /**
         * Sets the font-family override.
         *
         * @param value built-in or registered family name; null clears the override
         * @return this builder
         */
        public Builder fontFamily(String value) {
            fontFamily = value;
            return this;
        }

        /**
         * Sets the font size in points (1/72 inch). Generation requires a positive finite size.
         *
         * @param value font size in points
         * @return this builder
         */
        public Builder fontSizePt(double value) {
            fontSizePt = value;
            return this;
        }

        /**
         * Sets the text-color override. Generation requires six hexadecimal RGB digits prefixed by
         * {@code #} when a color is supplied.
         *
         * @param value RGB color such as {@code #123ABC}; null clears the override
         * @return this builder
         */
        public Builder color(String value) {
            color = value;
            return this;
        }

        /**
         * Enables bold, overriding an inherited regular weight.
         *
         * @return this builder
         */
        public Builder bold() {
            return bold(true);
        }

        /**
         * Overrides the inherited bold flag, including explicitly disabling it.
         *
         * @param value whether to render bold text
         * @return this builder
         */
        public Builder bold(boolean value) {
            bold = value;
            return this;
        }

        /**
         * Enables italic, overriding an inherited upright style.
         *
         * @return this builder
         */
        public Builder italic() {
            return italic(true);
        }

        /**
         * Overrides the inherited italic flag, including explicitly disabling it.
         *
         * @param value whether to render italic text
         * @return this builder
         */
        public Builder italic(boolean value) {
            italic = value;
            return this;
        }

        /**
         * Enables underlining for text.
         *
         * @return this builder
         */
        public Builder underline() {
            underline = true;
            return this;
        }

        /**
         * Overrides the inherited underline flag, including explicitly disabling it.
         *
         * @param value whether to underline text
         * @return this builder
         */
        public Builder underline(boolean value) {
            underline = value;
            return this;
        }

        /**
         * Sets horizontal paragraph alignment.
         *
         * @param value alignment override; null clears the override
         * @return this builder
         */
        public Builder textAlign(TextAlign value) {
            textAlign = value;
            return this;
        }

        /**
         * Sets the dimensionless line-height multiplier, applied to font size. Generation requires
         * a positive finite value. Text layout accommodates the largest run line box.
         *
         * @param value font-size multiplier, such as 1.2
         * @return this builder
         */
        public Builder lineHeight(double value) {
            lineHeight = value;
            return this;
        }

        /**
         * Sets spacing before a block. Generation requires a finite nonnegative value; cell text
         * always uses zero paragraph spacing.
         *
         * @param value spacing in millimeters
         * @return this builder
         */
        public Builder spaceBeforeMm(double value) {
            spaceBeforeMm = value;
            return this;
        }

        /**
         * Sets spacing after a block. Generation requires a finite nonnegative value; cell text
         * always uses zero paragraph spacing.
         *
         * @param value spacing in millimeters
         * @return this builder
         */
        public Builder spaceAfterMm(double value) {
            spaceAfterMm = value;
            return this;
        }

        /**
         * Snapshots the accumulated overrides without validating them or replacing nulls. The
         * builder can be changed and reused after this call.
         *
         * @return a new immutable style with the current overrides
         */
        public Style build() {
            return new Style(
                    fontFamily,
                    fontSizePt,
                    color,
                    bold,
                    italic,
                    underline,
                    textAlign,
                    lineHeight,
                    spaceBeforeMm,
                    spaceAfterMm);
        }
    }
}
