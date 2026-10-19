package com.nsdev.lettergenerator.internal.style;

import com.nsdev.lettergenerator.font.FontVariant;
import com.nsdev.lettergenerator.internal.font.StandardFontFamilies;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.style.TextAlign;

/**
 * Immutable effective style shared by validation and rendering. Unlike nullable {@link Style},
 * every primitive property has a concrete value; this carrier does not itself validate values.
 *
 * @param fontFamily effective family identifier
 * @param fontSizePt effective font size in points
 * @param color effective hexadecimal text color
 * @param bold whether to select a bold face
 * @param italic whether to select an italic face
 * @param underline whether to underline text
 * @param textAlign effective text alignment
 * @param lineHeight unitless line-height multiplier
 * @param spaceBeforeMm preceding block spacing in millimeters
 * @param spaceAfterMm following block spacing in millimeters
 */
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
    /** Default paragraph font size in points. */
    private static final double DEFAULT_FONT_SIZE_PT = 11.0;

    /** Default heading font size in points. */
    private static final double DEFAULT_HEADING_SIZE_PT = 14.0;

    /** Default unitless line-height multiplier. */
    private static final double DEFAULT_LINE_HEIGHT = 1.2;

    /** Default text color. */
    private static final String DEFAULT_COLOR = "#000000";

    /**
     * Starts a resolution chain with Helvetica, black, left alignment and 1.2 line height.
     *
     * @param heading true for bold 14 pt heading defaults; false for regular 11 pt text
     * @return defaults with no underline, italics or block spacing
     */
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

    /**
     * Overlays each non-null property; explicit false values override inherited true values.
     *
     * @param style local overrides, or null to retain this instance
     * @return effective style after property-by-property inheritance
     */
    public ResolvedStyle overlay(Style style) {
        if (style == null) {
            return this;
        }
        ResolvedStyle base = style.baseStyle() == null ? this : overlay(style.baseStyle());
        return new ResolvedStyle(
                inherited(style.fontFamily(), base.fontFamily),
                inherited(style.fontSizePt(), base.fontSizePt),
                inherited(style.color(), base.color),
                inherited(style.bold(), base.bold),
                inherited(style.italic(), base.italic),
                inherited(style.underline(), base.underline),
                inherited(style.textAlign(), base.textAlign),
                inherited(style.lineHeight(), base.lineHeight),
                inherited(style.spaceBeforeMm(), base.spaceBeforeMm),
                inherited(style.spaceAfterMm(), base.spaceAfterMm));
    }

    /**
     * Replaces only line height; validation remains the caller's responsibility.
     *
     * @param multiplier unitless line-height multiplier
     * @return copy with the supplied multiplier
     */
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

    /**
     * @return font face selected solely from the effective bold and italic flags
     */
    public FontVariant variant() {
        return FontVariant.fromStyle(bold, italic);
    }

    /**
     * Converts effective typography back into explicit model overrides for a nested content
     * context.
     *
     * @return immutable typography and spacing values; pagination controls remain local to blocks
     */
    public Style toStyle() {
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

    /**
     * Preserves a local non-null property, including false and zero, over its parent value.
     *
     * @param <T> property value type
     * @param value nullable local property override
     * @param fallback parent property used only when the override is null
     * @return non-null local value when present, otherwise the fallback
     */
    private static <T> T inherited(T value, T fallback) {
        return value == null ? fallback : value;
    }
}
