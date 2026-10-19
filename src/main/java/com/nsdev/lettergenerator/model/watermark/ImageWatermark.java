package com.nsdev.lettergenerator.model.watermark;

import com.nsdev.lettergenerator.internal.content.ImageBlocks;
import com.nsdev.lettergenerator.internal.defaults.WatermarkDefaults;
import com.nsdev.lettergenerator.model.layout.PageSelector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/**
 * PNG/JPEG/static SVG image centered behind selected physical pages, with its aspect ratio
 * preserved inside the supplied millimeter box. Watermark position is independent of page margins.
 * Opacity ranges from zero to one; positive rotation turns counterclockwise about page center in
 * PDF coordinates.
 *
 * <p>The constructor copies image bytes but does not decode them or default null properties.
 * Generation checks the MIME type, signature, decodability, positive finite box dimensions, page
 * selection, opacity, and finite rotation. Factories detect image format and provide complete
 * defaults. Stored bytes are protected by defensive copies on construction and access; equality and
 * hashing compare their contents.
 *
 * @param data encoded PNG/JPEG/static SVG bytes, required during generation
 * @param mimeType exact supported MIME type, {@code image/png}, {@code image/jpeg} or {@code
 *     image/svg+xml}
 * @param widthMm image fitting-box width in millimeters; required and positive during generation
 * @param heightMm image fitting-box height in millimeters; required and positive during generation
 * @param pages page selection, required during generation
 * @param opacity finite paint opacity between zero and one, required during generation
 * @param rotationDegrees finite counterclockwise angle in degrees, required during generation
 */
public record ImageWatermark(
        byte[] data,
        String mimeType,
        Double widthMm,
        Double heightMm,
        PageSelector pages,
        Double opacity,
        Double rotationDegrees)
        implements Watermark {
    /** Image factory rotation about page center, in degrees. */
    private static final double DEFAULT_ROTATION_DEGREES = 0.0;

    /**
     * Decodes supplied image bytes, detects PNG/JPEG/static SVG format, and proportionally fits the
     * image within an 80 by 40 mm box. The result selects all pages with opacity 0.12 and zero
     * rotation. Unlike direct record construction, this factory validates image readability.
     *
     * @param data encoded image bytes, copied before decoding
     * @return watermark with fitted dimensions and default page/paint settings
     * @throws IllegalArgumentException if data is null, unreadable, or not supported
     *     PNG/JPEG/static SVG
     */
    public static ImageWatermark of(byte[] data) {
        var image =
                ImageBlocks.fit(
                        data,
                        WatermarkDefaults.IMAGE_MAX_WIDTH_MM,
                        WatermarkDefaults.IMAGE_MAX_HEIGHT_MM);
        return new ImageWatermark(
                image.data(),
                image.mimeType(),
                image.widthMm(),
                image.heightMm(),
                PageSelector.ALL,
                WatermarkDefaults.OPACITY,
                DEFAULT_ROTATION_DEGREES);
    }

    /**
     * Reads a local image and creates the same fitted default watermark as {@link #of(byte[])}. The
     * file is fully read before the watermark is returned.
     *
     * @param path local PNG/JPEG/static SVG file
     * @return watermark fitted within 80 by 40 mm, on all pages, with opacity 0.12 and no rotation
     * @throws IOException if the image file cannot be read
     * @throws NullPointerException if path is null
     * @throws IllegalArgumentException if the file is not a readable supported PNG/JPEG/static SVG
     */
    public static ImageWatermark of(Path path) throws IOException {
        return of(Files.readAllBytes(path));
    }

    /**
     * Defensively copies the encoded bytes. Other components, including nulls, are stored unchanged
     * and validated during generation.
     *
     * @param data bytes to clone, or null
     * @param mimeType declared image format
     * @param widthMm fitting-box width in millimeters
     * @param heightMm fitting-box height in millimeters
     * @param pages page selection
     * @param opacity paint opacity from zero to one
     * @param rotationDegrees counterclockwise rotation in degrees
     */
    public ImageWatermark {
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
     * @return true for an image watermark with equal bytes and other components
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ImageWatermark image)) {
            return false;
        }
        return Arrays.equals(data, image.data)
                && Objects.equals(mimeType, image.mimeType)
                && Objects.equals(widthMm, image.widthMm)
                && Objects.equals(heightMm, image.heightMm)
                && Objects.equals(pages, image.pages)
                && Objects.equals(opacity, image.opacity)
                && Objects.equals(rotationDegrees, image.rotationDegrees);
    }

    /**
     * Hashes byte contents and every other record component consistently with equality.
     *
     * @return content-based hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                Arrays.hashCode(data),
                mimeType,
                widthMm,
                heightMm,
                pages,
                opacity,
                rotationDegrees);
    }
}
