package com.nsdev.lettergenerator.internal.font;

import com.nsdev.lettergenerator.font.FontVariant;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/** Immutable font bytes; callers receive read-only streams rather than the backing array. */
public final class RegisteredFont {
    /** Configured case-sensitive family name. */
    private final String family;

    /** Configured font face variant. */
    private final FontVariant variant;

    /** Owned encoded font bytes, exposed only through independent streams. */
    private final byte[] data;

    /**
     * Takes a defensive byte snapshot after {@link FontRegistry} has verified the face.
     *
     * @param family case-sensitive configured family identifier
     * @param variant configured font face variant
     * @param data font bytes to copy into owned storage
     */
    RegisteredFont(String family, FontVariant variant, byte[] data) {
        this.family = family;
        this.variant = variant;
        this.data = data.clone();
    }

    /**
     * @return the case-sensitive configured family identifier
     */
    public String family() {
        return family;
    }

    /**
     * @return this registration's face variant
     */
    public FontVariant variant() {
        return variant;
    }

    /**
     * Opens an independent stream positioned at the start; no backing byte array is exposed.
     *
     * @return new stream over the owned bytes, to be closed by the caller
     */
    public InputStream openStream() {
        return new ByteArrayInputStream(data);
    }
}
