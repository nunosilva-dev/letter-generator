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
    /** Binary sfnt signature for a TrueType font with TrueType outlines. */
    private static final byte[] TRUE_TYPE_SIGNATURE = {0, 1, 0, 0};

    /** Case-sensitive configured family plus face variant; file metadata does not rename it. */
    private record FontKey(String family, FontVariant variant) {}

    /**
     * Immutable custom registrations; built-in faces are represented by family identifiers only.
     */
    private final Map<FontKey, RegisteredFont> fonts;

    /**
     * Snapshots validated registrations, allowing the resulting registry to be shared.
     *
     * @param fonts validated custom registrations keyed by family and face
     */
    private FontRegistry(Map<FontKey, RegisteredFont> fonts) {
        this.fonts = Map.copyOf(fonts);
    }

    /**
     * @return a registry accepting only the built-in families
     */
    public static FontRegistry empty() {
        return new FontRegistry(Map.of());
    }

    /**
     * Reads configured sources once and validates a complete custom-font snapshot. Sources must
     * have a nonblank family, a variant, unique family/variant keys and TrueType outlines; every
     * custom family must include a regular face. Built-in names cannot be replaced.
     *
     * @param sources non-null trusted source list without null entries
     * @return immutable registry owning the loaded font bytes
     * @throws InvalidLetterException if registration, I/O or TrueType validation fails
     */
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

    /**
     * Checks that a style names a built-in family or a registered regular custom face.
     *
     * @param family case-sensitive family identifier
     * @param fieldPath caller's model path for any error
     * @throws InvalidLetterException if the family is unregistered, including null
     */
    public void requireFamily(String family, String fieldPath) {
        if (!StandardFontFamilies.contains(family)
                && !fonts.containsKey(new FontKey(family, FontVariant.REGULAR))) {
            throw new InvalidLetterException(fieldPath, "Unregistered font family: " + family);
        }
    }

    /**
     * Checks the exact custom face; built-in families provide all supported variants.
     *
     * @param family case-sensitive family identifier
     * @param variant required bold/italic combination
     * @param fieldPath caller's model path for any error
     * @throws InvalidLetterException if family or custom face is unavailable
     */
    public void requireVariant(String family, FontVariant variant, String fieldPath) {
        requireFamily(family, fieldPath);
        if (!StandardFontFamilies.contains(family)
                && !fonts.containsKey(new FontKey(family, variant))) {
            throw new InvalidLetterException(
                    fieldPath, "Unregistered font variant: " + family + " / " + variant);
        }
    }

    /**
     * @return unmodifiable custom-face collection; iteration order is unspecified
     */
    public Collection<RegisteredFont> registeredFonts() {
        return fonts.values();
    }

    /**
     * Looks up custom regular bytes for consumers such as watermark rendering.
     *
     * @param family case-sensitive family identifier
     * @return custom regular face, or empty for built-ins and unknown identifiers
     */
    public Optional<RegisteredFont> regular(String family) {
        return Optional.ofNullable(fonts.get(new FontKey(family, FontVariant.REGULAR)));
    }

    /**
     * Checks configuration names before any font I/O and prevents built-in replacement.
     *
     * @param source trusted font source to read
     */
    private static void validateSource(FontSource source) {
        if (source.family() == null || source.family().isBlank() || source.variant() == null) {
            throw new InvalidLetterException("fonts", "family and variant are required");
        }
        if (StandardFontFamilies.contains(source.family())) {
            throw new InvalidLetterException("fonts", "cannot replace a standard family");
        }
    }

    /**
     * Requires the TrueType-outline signature, then asks AWT to validate the complete font. I/O and
     * format failures retain the configured family path and original cause.
     *
     * @param source trusted font source to read
     * @return validated registration owning copied font bytes
     */
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

    /**
     * Rejects incomplete families after all sources are loaded, so source ordering is irrelevant.
     *
     * @param fonts validated custom registrations keyed by family and face
     */
    private static void requireRegularFaces(Map<FontKey, RegisteredFont> fonts) {
        for (FontKey key : fonts.keySet()) {
            if (!fonts.containsKey(new FontKey(key.family(), FontVariant.REGULAR))) {
                throw new InvalidLetterException(
                        path(key.family()), "A regular variant is required");
            }
        }
    }

    /**
     * Checks a binary signature without reading past a short input array.
     *
     * @param bytes encoded byte sequence to inspect
     * @param signature required leading byte signature
     * @return whether the bytes begin with the complete signature
     */
    private static boolean startsWith(byte[] bytes, byte[] signature) {
        return bytes.length >= signature.length
                && Arrays.equals(bytes, 0, signature.length, signature, 0, signature.length);
    }

    /**
     * Builds the registration path used by font loading diagnostics.
     *
     * @param family family whose configuration failed
     * @return font-family diagnostic path
     */
    private static String path(String family) {
        return "fonts[" + family + "]";
    }
}
