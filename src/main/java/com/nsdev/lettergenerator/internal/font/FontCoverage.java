package com.nsdev.lettergenerator.internal.font;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.font.FontVariant;
import java.awt.Font;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.fop.fonts.Base14Font;
import org.apache.fop.fonts.base14.Courier;
import org.apache.fop.fonts.base14.Helvetica;
import org.apache.fop.fonts.base14.TimesRoman;

/**
 * Per-preparation glyph coverage from only explicitly owned fonts and native built-in encodings.
 */
final class FontCoverage {
    /**
     * Exact configured face identity.
     *
     * @param family configured case-sensitive family
     * @param variant required face variant
     */
    private record Face(String family, FontVariant variant) {}

    /** Parsed custom fonts confined to this preparation invocation. */
    private final Map<Face, Font> custom = new HashMap<>();

    /** Native built-in coverage independent of host font installation. */
    private final Map<String, Base14Font> standard =
            Map.of(
                    StandardFontFamilies.HELVETICA, new Helvetica(),
                    StandardFontFamilies.TIMES, new TimesRoman(),
                    StandardFontFamilies.COURIER, new Courier());

    /**
     * Parses the already validated owned bytes without system font lookup.
     *
     * @param registry immutable registered faces
     */
    FontCoverage(FontRegistry registry) {
        for (RegisteredFont face : registry.registeredFonts()) {
            try (InputStream input = face.openStream()) {
                custom.put(
                        new Face(face.family(), face.variant()),
                        Font.createFont(Font.TRUETYPE_FONT, input));
            } catch (Exception exception) {
                throw new InvalidLetterException(
                        "fonts", "Unable to inspect registered glyphs", exception);
            }
        }
    }

    /**
     * Checks the entire grapheme in one exact face, ignoring only preserved layout whitespace.
     *
     * @param family configured family
     * @param variant required face
     * @param cluster complete Unicode grapheme cluster
     * @return whether one configured face covers every visible code point
     */
    boolean supports(String family, FontVariant variant, String cluster) {
        Font font = custom.get(new Face(family, variant));
        Base14Font builtin = standard.get(family);
        if (font == null && builtin == null) {
            return false;
        }
        return cluster.codePoints()
                .allMatch(
                        codePoint -> {
                            if (codePoint == '\n' || codePoint == '\r' || codePoint == '\t') {
                                return true;
                            }
                            return font != null
                                    ? font.canDisplay(codePoint)
                                    : Character.isBmpCodePoint(codePoint)
                                            && builtin.hasChar((char) codePoint);
                        });
    }
}
