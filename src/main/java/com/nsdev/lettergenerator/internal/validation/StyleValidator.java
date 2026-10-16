package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.internal.font.FontRegistry;
import com.nsdev.lettergenerator.model.style.Style;
import java.util.regex.Pattern;

final class StyleValidator {
    private static final Pattern RGB_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");
    private final FontRegistry fonts;

    StyleValidator(FontRegistry fonts) {
        this.fonts = fonts;
    }

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

    static void color(String value, String path) {
        ValidationChecks.require(
                value != null && RGB_COLOR.matcher(value).matches(), path, "expected #RRGGBB");
    }
}
