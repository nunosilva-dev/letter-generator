package com.nsdev.lettergenerator.internal.content;

import java.util.Arrays;

/** Supported encoded-image identifiers and signatures shared by fitting and validation. */
public enum ImageFormat {
    /** Portable Network Graphics with its complete eight-byte signature. */
    PNG("png", "image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),

    /** JPEG with the start-of-image and following marker prefix. */
    JPEG("jpeg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),

    /** Static SVG checked by the secure parser rather than a fixed binary prefix. */
    SVG("svg", "image/svg+xml", new byte[0]);

    /** Lowercase format name reported by an ImageIO reader. */
    private final String readerName;

    /** Exact MIME label accepted by the document model. */
    private final String mimeType;

    /** Owned format prefix, never exposed to callers. */
    private final byte[] signature;

    /**
     * Captures intrinsic format identifiers and snapshots its expected prefix.
     *
     * @param readerName lowercase ImageIO format identifier
     * @param mimeType exact supported MIME label
     * @param signature expected encoded-byte prefix
     */
    ImageFormat(String readerName, String mimeType, byte[] signature) {
        this.readerName = readerName;
        this.mimeType = mimeType;
        this.signature = signature.clone();
    }

    /**
     * Looks up an already-normalized ImageIO reader name without accepting aliases.
     *
     * @param readerName lowercase reader name, or null
     * @return supported format, or null when not supported
     */
    static ImageFormat fromReaderName(String readerName) {
        for (ImageFormat format : values()) {
            if (format.readerName.equals(readerName)) {
                return format;
            }
        }
        return null;
    }

    /**
     * Looks up an exact model MIME label without case folding or alias expansion.
     *
     * @param mimeType declared MIME label, or null
     * @return supported format, or null when not supported
     */
    public static ImageFormat fromMimeType(String mimeType) {
        for (ImageFormat format : values()) {
            if (format.mimeType.equals(mimeType)) {
                return format;
            }
        }
        return null;
    }

    /**
     * Returns the MIME label used when a readable image is fitted into a model block.
     *
     * @return exact supported MIME label
     */
    public String mimeType() {
        return mimeType;
    }

    /**
     * Checks the encoded prefix without decoding or reading beyond either byte array.
     *
     * @param data non-null encoded bytes; short arrays fail the match
     * @return true when the complete format prefix matches
     * @throws NullPointerException if data is null
     */
    public boolean matchesSignature(byte[] data) {
        if (this == SVG) {
            try {
                SvgImages.inspect(data);
                return true;
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
        return data.length >= signature.length
                && Arrays.equals(data, 0, signature.length, signature, 0, signature.length);
    }
}
