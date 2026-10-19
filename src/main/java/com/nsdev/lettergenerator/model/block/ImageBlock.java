package com.nsdev.lettergenerator.model.block;

import com.nsdev.lettergenerator.model.style.Style;
import java.util.Arrays;
import java.util.Objects;

/**
 * PNG/JPEG/static SVG image scaled proportionally to fit an explicit box in millimeters. The
 * rendered image participates in body flow or the containing positioned region. The block style
 * controls alignment and surrounding spacing. Generation checks the MIME type, signature,
 * decodability, positive finite dimensions, and available width.
 *
 * <p>The constructor and {@link #data()} clone bytes, so callers cannot mutate stored image
 * content. Equality and hashing use byte contents along with all other record components. The
 * constructor itself does not validate the image.
 *
 * @param data encoded PNG/JPEG/static SVG bytes; null is retained for generation-time validation
 * @param mimeType exact supported MIME type, {@code image/png}, {@code image/jpeg} or {@code
 *     image/svg+xml}
 * @param widthMm maximum rendered width in millimeters; required and positive at generation
 * @param heightMm maximum rendered height in millimeters; required and positive at generation
 * @param style nullable block overrides for alignment and spacing
 * @param altText optional human-readable image description for tagged output
 */
public record ImageBlock(
        byte[] data, String mimeType, Double widthMm, Double heightMm, Style style, String altText)
        implements Block {
    /**
     * Defensively copies image bytes without decoding or validating the supplied properties.
     *
     * @param data bytes to clone, or null
     * @param mimeType declared image MIME type
     * @param widthMm image box width in millimeters
     * @param heightMm image box height in millimeters
     * @param style optional block overrides
     * @param altText optional accessible image description
     */
    public ImageBlock {
        data = data == null ? null : data.clone();
    }

    /**
     * Preserves construction without an image description.
     *
     * @param data image bytes
     * @param mimeType declared format
     * @param widthMm image box width
     * @param heightMm image box height
     * @param style optional style
     */
    public ImageBlock(byte[] data, String mimeType, Double widthMm, Double heightMm, Style style) {
        this(data, mimeType, widthMm, heightMm, style, null);
    }

    /**
     * Replaces the description while preserving the image and its geometry.
     *
     * @param value optional accessible description
     * @return independent image block
     */
    public ImageBlock withAltText(String value) {
        return new ImageBlock(data, mimeType, widthMm, heightMm, style, value);
    }

    /**
     * Replaces local style overrides while preserving the description.
     *
     * @param value new style
     * @return independent image block
     */
    public ImageBlock withStyle(Style value) {
        return new ImageBlock(data, mimeType, widthMm, heightMm, value, altText);
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
                && Objects.equals(style, image.style)
                && Objects.equals(altText, image.altText);
    }

    /**
     * Hashes byte contents and every other record component consistently with equality.
     *
     * @return content-based hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(data), mimeType, widthMm, heightMm, style, altText);
    }
}
