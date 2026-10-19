package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.internal.font.FontRegistry;
import com.nsdev.lettergenerator.internal.style.RgbColors;
import com.nsdev.lettergenerator.model.style.Style;

/**
 * Validates only explicitly set style properties; null properties retain inheritance. Variant
 * availability is checked after block/run styles have been resolved.
 */
final class StyleValidator {
    /** Generator font snapshot for checking explicitly selected families. */
    private final FontRegistry fonts;

    /**
     * Binds style-family checks to the generator font registry.
     *
     * @param fonts font families available to the generator
     */
    StyleValidator(FontRegistry fonts) {
        this.fonts = fonts;
    }

    /**
     * Accepts null/inherited styles; checks registered families, finite positive font size and line
     * height, nonnegative spacing and normalized RGB colors.
     *
     * @param style nullable override set; null means all properties inherit
     * @param path model-field path used to locate any validation error
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if a supplied family,
     *     size, line height, spacing, or color is invalid
     */
    void validate(Style style, String path) {
        if (style == null) {
            return;
        }
        if (style.fontFamily() != null) {
            fonts.requireFamily(style.fontFamily(), path + ".fontFamily");
        }
        if (style.fontSizePt() != null) {
            ValidationChecks.positive(style.fontSizePt(), path + ".fontSizePt");
        }
        if (style.lineHeight() != null) {
            ValidationChecks.positive(style.lineHeight(), path + ".lineHeight");
        }
        if (style.spaceBeforeMm() != null) {
            ValidationChecks.nonnegative(style.spaceBeforeMm(), path + ".spaceBeforeMm");
        }
        if (style.spaceAfterMm() != null) {
            ValidationChecks.nonnegative(style.spaceAfterMm(), path + ".spaceAfterMm");
        }
        if (style.color() != null) {
            color(style.color(), path + ".color");
        }
    }

    /**
     * Requires a non-null six-digit RGB value in #RRGGBB form; markup color names have already been
     * normalized by the HTML style reader.
     *
     * @param value required model RGB value, including the leading hash
     * @param path model-field path used to locate any validation error
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if the value is null or
     *     does not match six-digit hexadecimal RGB syntax
     */
    static void color(String value, String path) {
        ValidationChecks.require(RgbColors.isHexRgb(value), path, "expected #RRGGBB");
    }
}
