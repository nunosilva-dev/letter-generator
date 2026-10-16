package com.nsdev.lettergenerator.internal.font;

import com.nsdev.lettergenerator.font.FontVariant;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/** Immutable font bytes; callers receive read-only streams rather than the backing array. */
public final class RegisteredFont {
    private final String family;
    private final FontVariant variant;
    private final byte[] data;

    RegisteredFont(String family, FontVariant variant, byte[] data) {
        this.family = family;
        this.variant = variant;
        this.data = data.clone();
    }

    public String family() {
        return family;
    }

    public FontVariant variant() {
        return variant;
    }

    public InputStream openStream() {
        return new ByteArrayInputStream(data);
    }
}
