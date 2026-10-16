package com.nsdev.lettergenerator.internal.font;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.font.FontVariant;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Validated snapshot of registered TrueType faces, safe to share between generation calls. */
public final class FontRegistry {
    private static final byte[] TRUE_TYPE_SIGNATURE = {0, 1, 0, 0};

    private record FontKey(String family, FontVariant variant) {}

    private final Map<FontKey, RegisteredFont> fonts;

    private FontRegistry(Map<FontKey, RegisteredFont> fonts) {
        this.fonts = Map.copyOf(fonts);
    }

    public static FontRegistry empty() {
        return new FontRegistry(Map.of());
    }

    public static FontRegistry load(List<FontSource> sources) {
        Map<FontKey, RegisteredFont> fonts = new LinkedHashMap<>();
        for (FontSource source : sources) {
            validateSource(source);
            RegisteredFont font = loadFont(source);
            FontKey key = new FontKey(font.family(), font.variant());
            if (fonts.putIfAbsent(key, font) != null) {
                throw new InvalidLetterException(
                        path(font.family()), "Duplicate family/variant: " + font.variant());
            }
        }
        requireRegularFaces(fonts);
        return new FontRegistry(fonts);
    }

    public void requireFamily(String family, String fieldPath) {
        if (!StandardFontFamilies.contains(family)
                && !fonts.containsKey(new FontKey(family, FontVariant.REGULAR))) {
            throw new InvalidLetterException(fieldPath, "Unregistered font family: " + family);
        }
    }

    public void requireVariant(String family, FontVariant variant, String fieldPath) {
        requireFamily(family, fieldPath);
        if (!StandardFontFamilies.contains(family)
                && !fonts.containsKey(new FontKey(family, variant))) {
            throw new InvalidLetterException(
                    fieldPath, "Unregistered font variant: " + family + " / " + variant);
        }
    }

    public Collection<RegisteredFont> registeredFonts() {
        return fonts.values();
    }

    public Optional<RegisteredFont> regular(String family) {
        return Optional.ofNullable(fonts.get(new FontKey(family, FontVariant.REGULAR)));
    }

    private static void validateSource(FontSource source) {
        if (source.family() == null || source.family().isBlank() || source.variant() == null) {
            throw new InvalidLetterException("fonts", "family and variant are required");
        }
        if (StandardFontFamilies.contains(source.family())) {
            throw new InvalidLetterException("fonts", "cannot replace a standard family");
        }
    }

    private static RegisteredFont loadFont(FontSource source) {
        try {
            byte[] bytes = source.readBytes();
            if (!startsWith(bytes, TRUE_TYPE_SIGNATURE)) {
                throw new IOException("Expected a TrueType font with TrueType outlines");
            }
            try (InputStream input = new ByteArrayInputStream(bytes)) {
                Font.createFont(Font.TRUETYPE_FONT, input);
            }
            return new RegisteredFont(source.family(), source.variant(), bytes);
        } catch (IOException | FontFormatException exception) {
            throw new InvalidLetterException(
                    path(source.family()), "Unable to load font", exception);
        }
    }

    private static void requireRegularFaces(Map<FontKey, RegisteredFont> fonts) {
        for (FontKey key : fonts.keySet()) {
            if (!fonts.containsKey(new FontKey(key.family(), FontVariant.REGULAR))) {
                throw new InvalidLetterException(
                        path(key.family()), "A regular variant is required");
            }
        }
    }

    private static boolean startsWith(byte[] bytes, byte[] signature) {
        return bytes.length >= signature.length
                && Arrays.equals(bytes, 0, signature.length, signature, 0, signature.length);
    }

    private static String path(String family) {
        return "fonts[" + family + "]";
    }
}
