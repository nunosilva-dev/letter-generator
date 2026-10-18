package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.model.style.Style;
import java.util.Arrays;
import java.util.Objects;

/**
 * PNG/JPEG image scaled proportionally to fit an explicit box in millimeters. The rendered image
 * participates in body flow or the containing positioned region. The block style controls alignment
 * and surrounding spacing. Generation checks the MIME type, signature, decodability, positive
 * finite dimensions, and available width.
 *
 * <p>The constructor and {@link #data()} clone bytes, so callers cannot mutate stored image
 * content. Equality and hashing use byte contents along with all other record components. The
 * constructor itself does not validate the image.
 *
 * @param data encoded PNG/JPEG bytes; null is retained for generation-time validation
 * @param mimeType exact supported MIME type, {@code image/png} or {@code image/jpeg}
 * @param widthMm maximum rendered width in millimeters; required and positive at generation
 * @param heightMm maximum rendered height in millimeters; required and positive at generation
 * @param style nullable block overrides for alignment and spacing
 */
public record ImageBlock(byte[] data, String mimeType, Double widthMm, Double heightMm, Style style)
        implements Block {
    /**
     * Defensively copies image bytes without decoding or validating the supplied properties.
     *
     * @param data bytes to clone, or null
     * @param mimeType declared image MIME type
     * @param widthMm image box width in millimeters
     * @param heightMm image box height in millimeters
     * @param style optional block overrides
     */
    public ImageBlock {
        data = data == null ? null : data.clone();
    }

    /**
     * Returns a defensive copy of the encoded image.
     *
     * @return a new byte array, or null when no bytes were supplied
     */
    @Override
    public byte[] data() {
        return data == null ? null : data.clone();
    }

    /**
     * Compares image byte contents and every other record component.
     *
     * @param other object to compare, possibly null
     * @return true for an image block with equal bytes and other components
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ImageBlock image)) {
            return false;
        }
        return Arrays.equals(data, image.data)
                && Objects.equals(mimeType, image.mimeType)
                && Objects.equals(widthMm, image.widthMm)
                && Objects.equals(heightMm, image.heightMm)
                && Objects.equals(style, image.style);
    }

    /**
     * Hashes byte contents and every other record component consistently with equality.
     *
     * @return content-based hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(data), mimeType, widthMm, heightMm, style);
    }
}
