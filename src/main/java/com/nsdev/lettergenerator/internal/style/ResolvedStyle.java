package com.nsdev.lettergenerator.internal.style;

import com.nsdev.lettergenerator.font.FontVariant;
import com.nsdev.lettergenerator.internal.font.StandardFontFamilies;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;

/** Immutable effective style after property-by-property inheritance. */
public record ResolvedStyle(
        String fontFamily,
        double fontSizePt,
        String color,
        boolean bold,
        boolean italic,
        boolean underline,
        TextAlign textAlign,
        double lineHeight,
        double spaceBeforeMm,
        double spaceAfterMm) {
    private static final double DEFAULT_FONT_SIZE_PT = 11.0;
    private static final double DEFAULT_HEADING_SIZE_PT = 14.0;
    private static final double DEFAULT_LINE_HEIGHT = 1.2;
    private static final String DEFAULT_COLOR = "#000000";

    public static ResolvedStyle defaults(boolean heading) {
        return new ResolvedStyle(
                StandardFontFamilies.HELVETICA,
                heading ? DEFAULT_HEADING_SIZE_PT : DEFAULT_FONT_SIZE_PT,
                DEFAULT_COLOR,
                heading,
                false,
                false,
                TextAlign.LEFT,
                DEFAULT_LINE_HEIGHT,
                0,
                0);
    }

    public ResolvedStyle overlay(Style style) {
        if (style == null) {
            return this;
        }
        return new ResolvedStyle(
                inherited(style.fontFamily(), fontFamily),
                        inherited(style.fontSizePt(), fontSizePt),
                inherited(style.color(), color), inherited(style.bold(), bold),
                inherited(style.italic(), italic), inherited(style.underline(), underline),
                inherited(style.textAlign(), textAlign), inherited(style.lineHeight(), lineHeight),
                inherited(style.spaceBeforeMm(), spaceBeforeMm),
                        inherited(style.spaceAfterMm(), spaceAfterMm));
    }

    public ResolvedStyle withLineHeight(double multiplier) {
        return new ResolvedStyle(
                fontFamily,
                fontSizePt,
                color,
                bold,
                italic,
                underline,
                textAlign,
                multiplier,
                spaceBeforeMm,
                spaceAfterMm);
    }

    public FontVariant variant() {
        return FontVariant.fromStyle(bold, italic);
    }

    private static <T> T inherited(T value, T fallback) {
        return value == null ? fallback : value;
    }
}
