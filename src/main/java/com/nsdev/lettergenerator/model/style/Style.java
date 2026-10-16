package com.nsdev.lettergenerator.model.style;

/**
 * Nullable style properties. Absent values inherit from document, section, or block; sizes use
 * points and spacing uses millimeters.
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
    public static Style plain() {
        return new Style(null, null, null, null, null, null, null, null, null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Applies only properties explicitly supplied by the override. */
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

    private static <T> T inherited(T value, T fallback) {
        return value == null ? fallback : value;
    }

    /** Nullable overrides; omitted properties inherit rather than resetting document defaults. */
    public static final class Builder {
        private String fontFamily;
        private Double fontSizePt;
        private String color;
        private Boolean bold;
        private Boolean italic;
        private Boolean underline;
        private TextAlign textAlign;
        private Double lineHeight;
        private Double spaceBeforeMm;
        private Double spaceAfterMm;

        private Builder() {}

        public Builder fontFamily(String value) {
            fontFamily = value;
            return this;
        }

        public Builder fontSizePt(double value) {
            fontSizePt = value;
            return this;
        }

        public Builder color(String value) {
            color = value;
            return this;
        }

        public Builder bold() {
            return bold(true);
        }

        public Builder bold(boolean value) {
            bold = value;
            return this;
        }

        public Builder italic() {
            return italic(true);
        }

        public Builder italic(boolean value) {
            italic = value;
            return this;
        }

        public Builder underline() {
            underline = true;
            return this;
        }

        public Builder underline(boolean value) {
            underline = value;
            return this;
        }

        public Builder textAlign(TextAlign value) {
            textAlign = value;
            return this;
        }

        public Builder lineHeight(double value) {
            lineHeight = value;
            return this;
        }

        public Builder spaceBeforeMm(double value) {
            spaceBeforeMm = value;
            return this;
        }

        public Builder spaceAfterMm(double value) {
            spaceAfterMm = value;
            return this;
        }

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
